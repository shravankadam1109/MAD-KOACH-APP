// Storage security rules tests. Run only against the local emulators (Storage
// rules read profiles from the Firestore emulator).
import { after, before, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, setDoc } from 'firebase/firestore';
import { ref, uploadBytes, getBytes } from 'firebase/storage';

let env;
const bytes = new Uint8Array([1, 2, 3]);
const storage = (uid) => (uid ? env.authenticatedContext(uid) : env.unauthenticatedContext()).storage();

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-koach-rules',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'), host: '127.0.0.1', port: 8080 },
    storage: { rules: readFileSync(new URL('../storage.rules', import.meta.url), 'utf8'), host: '127.0.0.1', port: 9199 }
  });
});

after(async () => { await env.cleanup(); });

beforeEach(async () => {
  await env.clearFirestore();
  await env.clearStorage();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const s = ctx.firestore();
    await setDoc(doc(s, 'users/team'), { role: 'team', name: 'T', email: 't@test.koach' });
    await setDoc(doc(s, 'users/maya'), { role: 'client', name: 'A', email: 'a@client.test', clientId: 'cl-a' });
    await setDoc(doc(s, 'users/dev'), { role: 'client', name: 'B', email: 'b@client.test', clientId: 'cl-b' });
    await uploadBytes(ref(ctx.storage(), 'documents/cl-a/doc-1/brief.pdf'), bytes);
    await uploadBytes(ref(ctx.storage(), 'deliverables/cl-a/p-a/d-1/v1.0/cut.mp4'), bytes);
  });
});

describe('storage', () => {
  test("clients read their organisation's files, never another's", async () => {
    await assertSucceeds(getBytes(ref(storage('maya'), 'documents/cl-a/doc-1/brief.pdf')));
    await assertSucceeds(getBytes(ref(storage('maya'), 'deliverables/cl-a/p-a/d-1/v1.0/cut.mp4')));
    await assertFails(getBytes(ref(storage('dev'), 'documents/cl-a/doc-1/brief.pdf')));
    await assertFails(getBytes(ref(storage('dev'), 'deliverables/cl-a/p-a/d-1/v1.0/cut.mp4')));
    await assertFails(getBytes(ref(storage(null), 'documents/cl-a/doc-1/brief.pdf')));
  });

  test('clients upload documents only into their own organisation', async () => {
    await assertSucceeds(uploadBytes(ref(storage('maya'), 'documents/cl-a/doc-2/notes.pdf'), bytes));
    await assertFails(uploadBytes(ref(storage('maya'), 'documents/cl-b/doc-3/notes.pdf'), bytes));
  });

  test('only the agency uploads deliverables', async () => {
    await assertFails(uploadBytes(ref(storage('maya'), 'deliverables/cl-a/p-a/d-1/v2.0/cut.mp4'), bytes));
    await assertSucceeds(uploadBytes(ref(storage('team'), 'deliverables/cl-a/p-a/d-1/v2.0/cut.mp4'), bytes));
  });

  test('paths outside the known folders are denied', async () => {
    await assertFails(uploadBytes(ref(storage('team'), 'misc/anything.bin'), bytes));
  });
});
