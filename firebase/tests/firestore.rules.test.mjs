// Firestore security rules tests. Run only against the local emulator:
//   firebase emulators:exec --only firestore,storage "npm --prefix firebase test"
import { after, before, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, getDoc, getDocs, setDoc, updateDoc, deleteDoc, collection, addDoc, arrayUnion, query, where } from 'firebase/firestore';

let env;

const people = {
  admin: { role: 'admin', name: 'Test Admin', email: 'admin@test.koach' },
  team: { role: 'team', name: 'Test Editor', email: 'editor@test.koach' },
  maya: { role: 'client', name: 'Client A', email: 'a@client.test', clientId: 'cl-a' },
  dev: { role: 'client', name: 'Client B', email: 'b@client.test', clientId: 'cl-b' }
};

const db = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).firestore();

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-koach-rules',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'), host: '127.0.0.1', port: 8080 }
  });
});

after(async () => { await env.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const s = ctx.firestore();
    for (const [uid, p] of Object.entries(people)) await setDoc(doc(s, 'users', uid), p);
    await setDoc(doc(s, 'clients/cl-a'), { name: 'Org A' });
    await setDoc(doc(s, 'clients/cl-b'), { name: 'Org B' });
    await setDoc(doc(s, 'projects/p-a'), { title: 'A project', clientId: 'cl-a', status: 'IN_PROGRESS' });
    await setDoc(doc(s, 'projects/p-b'), { title: 'B project', clientId: 'cl-b', status: 'IN_PROGRESS' });
    await setDoc(doc(s, 'projects/p-a/deliverables/d-review'), { name: 'Cut v1', status: 'IN_REVIEW' });
    await setDoc(doc(s, 'projects/p-a/deliverables/d-draft'), { name: 'Rough', status: 'DRAFT' });
    await setDoc(doc(s, 'projects/p-a/revisions/r-1'), { reason: 'Warmer grade please', requestedBy: 'maya', resolved: false });
    await setDoc(doc(s, 'bookings/b-a'), { clientId: 'cl-a', requestedBy: 'maya', status: 'PENDING' });
    await setDoc(doc(s, 'bookings/b-confirmed'), { clientId: 'cl-a', requestedBy: 'maya', status: 'CONFIRMED' });
    await setDoc(doc(s, 'tasks/t-1'), { title: 'Grade', status: 'TODO' });
    await setDoc(doc(s, 'documents/doc-shared'), { clientId: 'cl-a', visibility: 'SHARED', uploadedBy: 'team' });
    await setDoc(doc(s, 'documents/doc-internal'), { clientId: 'cl-a', visibility: 'INTERNAL', uploadedBy: 'team' });
    await setDoc(doc(s, 'threads/th-a'), { projectId: 'p-a', type: 'PROJECT', participantIds: ['maya', 'team'] });
    await setDoc(doc(s, 'threads/th-legacy'), { projectId: 'p-a', participantIds: ['maya', 'team'] });
    await setDoc(doc(s, 'threads/th-a/messages/m-1'), { senderId: 'team', body: 'Hi', readBy: ['team'] });
    await setDoc(doc(s, 'threads/dm-maya-team'), { projectId: '', type: 'DM', title: 'DM', participantIds: ['maya', 'team'] });
    await setDoc(doc(s, 'threads/dm-maya-team/messages/m-1'), { senderId: 'maya', body: 'Private', readBy: ['maya'] });
    await setDoc(doc(s, 'notifications/n-maya'), { recipientId: 'maya', isRead: false, title: 'x' });
    await setDoc(doc(s, 'payments/pay-a'), { clientId: 'cl-a', status: 'DUE', amountInr: 1000 });
    await setDoc(doc(s, 'feedback/fb-a'), { clientId: 'cl-a', authorId: 'maya', status: 'OPEN' });
  });
});

describe('identity and roles', () => {
  test('signed-out users can read nothing', async () => {
    await assertFails(getDoc(doc(db(null), 'projects/p-a')));
    await assertFails(getDoc(doc(db(null), 'users/maya')));
  });

  test('a user cannot grant themselves a role or edit their profile', async () => {
    await assertFails(setDoc(doc(db('stranger'), 'users/stranger'), { role: 'admin', name: 'x', email: 'x@y.z' }));
    await assertFails(updateDoc(doc(db('maya'), 'users/maya'), { role: 'admin' }));
    await assertFails(updateDoc(doc(db('team'), 'users/team'), { role: 'admin' }));
  });

  test('only admins write profiles', async () => {
    await assertSucceeds(setDoc(doc(db('admin'), 'users/new'), { role: 'client', name: 'N', email: 'n@x.y', clientId: 'cl-a' }));
  });

  test('a new account can provision itself only as a client of its own organisation', async () => {
    const own = { role: 'client', name: 'New Client', email: 'new@x.y', clientId: 'cl-stranger', title: 'Client Partner', phone: '' };
    await assertFails(setDoc(doc(db('stranger'), 'users/stranger'), { ...own, role: 'team' }));
    await assertFails(setDoc(doc(db('stranger'), 'users/stranger'), { ...own, clientId: 'cl-a' }));
    await assertFails(setDoc(doc(db('stranger'), 'users/stranger'), { ...own, isAdmin: true }));
    await assertFails(setDoc(doc(db('stranger'), 'users/someone-else'), { ...own, clientId: 'cl-someone-else' }));
    await assertSucceeds(setDoc(doc(db('stranger'), 'users/stranger'), own));
    // Once provisioned, the role and organisation are fixed.
    await assertFails(updateDoc(doc(db('stranger'), 'users/stranger'), { role: 'admin' }));
    await assertFails(updateDoc(doc(db('stranger'), 'users/stranger'), { clientId: 'cl-a' }));
  });

  test('a self-provisioned client creates only its own organisation record, once', async () => {
    await assertFails(setDoc(doc(db('stranger'), 'clients/cl-stranger'), { name: 'Org', status: 'ACTIVE' }));
    await env.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), 'users/stranger'), { role: 'client', name: 'S', email: 's@x.y', clientId: 'cl-stranger' });
    });
    await assertFails(setDoc(doc(db('stranger'), 'clients/cl-a'), { name: 'Hijack', status: 'ACTIVE' }));
    await assertFails(setDoc(doc(db('stranger'), 'clients/cl-stranger'), { name: 'Org', status: 'ACTIVE', credit: 1 }));
    await assertSucceeds(setDoc(doc(db('stranger'), 'clients/cl-stranger'), { name: 'Org', contactName: 'S', email: 's@x.y', phone: '', status: 'ACTIVE', since: '2026-10-27' }));
    await assertFails(updateDoc(doc(db('stranger'), 'clients/cl-stranger'), { status: 'VIP' }));
  });

  test('anyone signed in can read their own profile (even if missing), but not others without a profile', async () => {
    await assertSucceeds(getDoc(doc(db('stranger'), 'users/stranger')));
    await assertFails(getDoc(doc(db('stranger'), 'users/maya')));
    await assertSucceeds(getDoc(doc(db('maya'), 'users/maya')));
  });

  test('a signed-in account without a profile has no access', async () => {
    await assertFails(getDoc(doc(db('stranger'), 'projects/p-a')));
    await assertFails(getDoc(doc(db('stranger'), 'tasks/t-1')));
  });
});

describe('client isolation', () => {
  test("clients read their own projects but never another client's", async () => {
    await assertSucceeds(getDoc(doc(db('maya'), 'projects/p-a')));
    await assertFails(getDoc(doc(db('maya'), 'projects/p-b')));
    await assertFails(getDoc(doc(db('dev'), 'projects/p-a')));
  });

  test("clients can't read another client's bookings, documents, payments or feedback", async () => {
    await assertFails(getDoc(doc(db('dev'), 'bookings/b-a')));
    await assertFails(getDoc(doc(db('dev'), 'documents/doc-shared')));
    await assertFails(getDoc(doc(db('dev'), 'payments/pay-a')));
    await assertFails(getDoc(doc(db('dev'), 'feedback/fb-a')));
    await assertFails(getDoc(doc(db('dev'), 'clients/cl-a')));
  });

  test('clients see shared documents but not internal ones', async () => {
    await assertSucceeds(getDoc(doc(db('maya'), 'documents/doc-shared')));
    await assertFails(getDoc(doc(db('maya'), 'documents/doc-internal')));
  });

  test('clients cannot see agency tasks', async () => {
    await assertFails(getDoc(doc(db('maya'), 'tasks/t-1')));
    await assertSucceeds(getDoc(doc(db('team'), 'tasks/t-1')));
  });

  test('threads written before DM support (no type) still open as project threads', async () => {
    await assertSucceeds(getDoc(doc(db('admin'), 'threads/th-legacy')));
    await assertSucceeds(getDoc(doc(db('maya'), 'threads/th-legacy')));
    await assertFails(getDoc(doc(db('dev'), 'threads/th-legacy')));
  });

  test("clients can't read other people's messages or notifications", async () => {
    await assertFails(getDoc(doc(db('dev'), 'threads/th-a/messages/m-1')));
    await assertFails(getDoc(doc(db('dev'), 'notifications/n-maya')));
    await assertSucceeds(getDoc(doc(db('maya'), 'threads/th-a/messages/m-1')));
  });
});

describe('projects and deliverables', () => {
  test('clients only flag a revision on their project; they cannot change anything else', async () => {
    await assertSucceeds(updateDoc(doc(db('maya'), 'projects/p-a'), { status: 'REVISION_REQUESTED' }));
    await assertFails(updateDoc(doc(db('maya'), 'projects/p-a'), { status: 'COMPLETED' }));
    await assertFails(updateDoc(doc(db('maya'), 'projects/p-a'), { title: 'Mine now' }));
    await assertFails(updateDoc(doc(db('dev'), 'projects/p-a'), { status: 'REVISION_REQUESTED' }));
  });

  test('clients approve deliverables in review; the agency cannot approve for them', async () => {
    await assertFails(updateDoc(doc(db('team'), 'projects/p-a/deliverables/d-review'), { status: 'APPROVED' }));
    await assertSucceeds(updateDoc(doc(db('maya'), 'projects/p-a/deliverables/d-review'), { status: 'APPROVED' }));
  });

  test('clients cannot see or approve drafts, and cannot edit deliverable details', async () => {
    await assertFails(getDoc(doc(db('maya'), 'projects/p-a/deliverables/d-draft')));
    await assertFails(updateDoc(doc(db('maya'), 'projects/p-a/deliverables/d-draft'), { status: 'APPROVED' }));
    await assertFails(updateDoc(doc(db('maya'), 'projects/p-a/deliverables/d-review'), { status: 'APPROVED', name: 'Renamed' }));
  });

  test('agency can share a draft for review', async () => {
    await assertSucceeds(updateDoc(doc(db('team'), 'projects/p-a/deliverables/d-draft'), { status: 'IN_REVIEW' }));
  });

  test('revision history is append-only, with requester fixed to the caller', async () => {
    const revisions = collection(db('maya'), 'projects/p-a/revisions');
    await assertSucceeds(addDoc(revisions, { reason: 'Tighten the end card', requestedBy: 'maya', resolved: false }));
    await assertFails(addDoc(revisions, { reason: 'Pretend it was Dev', requestedBy: 'dev', resolved: false }));
    await assertFails(addDoc(revisions, { reason: 'no', requestedBy: 'maya', resolved: false }));
    await assertFails(deleteDoc(doc(db('maya'), 'projects/p-a/revisions/r-1')));
    await assertFails(updateDoc(doc(db('team'), 'projects/p-a/revisions/r-1'), { reason: 'Edited' }));
    await assertSucceeds(updateDoc(doc(db('team'), 'projects/p-a/revisions/r-1'), { resolved: true }));
  });
});

describe('direct messages', () => {
  test('DMs are readable only by their two participants, even for admins', async () => {
    await assertSucceeds(getDoc(doc(db('maya'), 'threads/dm-maya-team')));
    await assertSucceeds(getDoc(doc(db('team'), 'threads/dm-maya-team/messages/m-1')));
    await assertFails(getDoc(doc(db('admin'), 'threads/dm-maya-team')));
    await assertFails(getDoc(doc(db('admin'), 'threads/dm-maya-team/messages/m-1')));
    await assertFails(getDoc(doc(db('dev'), 'threads/dm-maya-team')));
  });

  test("the agency lists project threads and its own threads, never everyone's", async () => {
    // An unfiltered list could include other people's DMs, so it's refused outright.
    await assertFails(getDocs(collection(db('admin'), 'threads')));
    const own = await getDocs(query(collection(db('maya'), 'threads'), where('participantIds', 'array-contains', 'maya')));
    if (!own.docs.some((d) => d.id === 'dm-maya-team')) throw new Error('participant should see their own DM');
    await assertSucceeds(getDocs(query(collection(db('admin'), 'threads'), where('type', '==', 'PROJECT'))));
    await assertSucceeds(getDocs(query(collection(db('admin'), 'threads'), where('participantIds', 'array-contains', 'admin'))));
  });
});

describe('bookings', () => {
  test('clients create pending bookings only for themselves and their organisation', async () => {
    const bookings = collection(db('maya'), 'bookings');
    await assertSucceeds(addDoc(bookings, { clientId: 'cl-a', requestedBy: 'maya', status: 'PENDING' }));
    await assertFails(addDoc(bookings, { clientId: 'cl-b', requestedBy: 'maya', status: 'PENDING' }));
    await assertFails(addDoc(bookings, { clientId: 'cl-a', requestedBy: 'maya', status: 'CONFIRMED' }));
  });

  test('agency follows allowed transitions and must give a decline reason', async () => {
    await assertFails(updateDoc(doc(db('team'), 'bookings/b-a'), { status: 'COMPLETED' }));
    await assertFails(updateDoc(doc(db('team'), 'bookings/b-a'), { status: 'DECLINED' }));
    await assertSucceeds(updateDoc(doc(db('team'), 'bookings/b-a'), { status: 'DECLINED', statusNote: 'Fully booked' }));
    await assertSucceeds(updateDoc(doc(db('team'), 'bookings/b-confirmed'), { status: 'COMPLETED' }));
  });

  test('clients can only cancel their own pending request', async () => {
    await assertSucceeds(updateDoc(doc(db('maya'), 'bookings/b-a'), { status: 'CANCELLED' }));
    await assertFails(updateDoc(doc(db('maya'), 'bookings/b-confirmed'), { status: 'CANCELLED' }));
    await assertFails(updateDoc(doc(db('maya'), 'bookings/b-a'), { status: 'CONFIRMED' }));
  });
});

describe('documents, messages, notifications, payments, feedback', () => {
  test('client uploads must be shared, in their organisation and attributed to them', async () => {
    const docs = collection(db('maya'), 'documents');
    await assertSucceeds(addDoc(docs, { clientId: 'cl-a', visibility: 'SHARED', uploadedBy: 'maya' }));
    await assertFails(addDoc(docs, { clientId: 'cl-a', visibility: 'INTERNAL', uploadedBy: 'maya' }));
    await assertFails(addDoc(docs, { clientId: 'cl-b', visibility: 'SHARED', uploadedBy: 'maya' }));
    await assertFails(updateDoc(doc(db('maya'), 'documents/doc-shared'), { visibility: 'INTERNAL' }));
  });

  test('messages are sent as yourself, within limits, and readBy only gains yourself', async () => {
    const messages = collection(db('maya'), 'threads/th-a/messages');
    await assertSucceeds(addDoc(messages, { senderId: 'maya', body: 'Looks great', readBy: ['maya'] }));
    await assertFails(addDoc(messages, { senderId: 'team', body: 'Spoofed', readBy: [] }));
    await assertFails(addDoc(messages, { senderId: 'maya', body: 'x'.repeat(1001), readBy: [] }));
    await assertSucceeds(updateDoc(doc(db('maya'), 'threads/th-a/messages/m-1'), { readBy: arrayUnion('maya') }));
    await assertFails(updateDoc(doc(db('maya'), 'threads/th-a/messages/m-1'), { readBy: ['team', 'maya', 'dev'] }));
    await assertFails(updateDoc(doc(db('maya'), 'threads/th-a/messages/m-1'), { body: 'Edited' }));
  });

  test('recipients only toggle isRead on their own notifications', async () => {
    await assertSucceeds(updateDoc(doc(db('maya'), 'notifications/n-maya'), { isRead: true }));
    await assertFails(updateDoc(doc(db('maya'), 'notifications/n-maya'), { title: 'changed' }));
    await assertFails(updateDoc(doc(db('dev'), 'notifications/n-maya'), { isRead: true }));
  });

  test('payment records are never written from a client device', async () => {
    await assertSucceeds(getDoc(doc(db('maya'), 'payments/pay-a')));
    await assertFails(updateDoc(doc(db('maya'), 'payments/pay-a'), { status: 'PAID' }));
    await assertFails(getDoc(doc(db('team'), 'payments/pay-a')));
    await assertSucceeds(updateDoc(doc(db('admin'), 'payments/pay-a'), { status: 'PAID' }));
  });

  test('feedback is created by clients as themselves; only the agency responds', async () => {
    const feedback = collection(db('maya'), 'feedback');
    await assertSucceeds(addDoc(feedback, { clientId: 'cl-a', authorId: 'maya', status: 'OPEN' }));
    await assertFails(addDoc(feedback, { clientId: 'cl-a', authorId: 'dev', status: 'OPEN' }));
    await assertFails(updateDoc(doc(db('maya'), 'feedback/fb-a'), { status: 'RESOLVED' }));
    await assertSucceeds(updateDoc(doc(db('team'), 'feedback/fb-a'), { status: 'RESOLVED', response: 'Fixed' }));
  });
});
