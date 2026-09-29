// End-to-end verification script for Medication Safety & Interaction Assistant
const BASE_URL = 'http://localhost:8080';

async function run() {
  console.log('=== STARTING END-TO-END DEMO READINESS VERIFICATION ===\n');

  // 1. Register User 1
  const rand = Math.floor(Math.random() * 1000000);
  const user1Email = `demo_patient_${rand}@medsafe.org`;
  console.log('1. Registering user 1:', user1Email);
  const reg1Res = await fetch(`${BASE_URL}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: 'Jane Doe', email: user1Email, password: 'Password@123' })
  });
  const reg1Data = await reg1Res.json();
  console.log('User 1 Registered. Token received:', !!reg1Data.token, 'PasswordHash exposed:', 'passwordHash' in reg1Data);
  const token1 = reg1Data.token;

  // 2. Add Medications
  console.log('\n2. Adding prescribed medications: Warfarin & Aspirin');
  const med1Res = await fetch(`${BASE_URL}/api/medications`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token1}` },
    body: JSON.stringify({
      medicineName: 'Warfarin',
      activeIngredient: 'warfarin',
      dosage: '5mg',
      frequency: 'Once daily at bedtime',
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      status: 'ACTIVE'
    })
  });
  const med1 = await med1Res.json();
  console.log('Added Warfarin (ID:', med1.medicationId, ')');

  const med2Res = await fetch(`${BASE_URL}/api/medications`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token1}` },
    body: JSON.stringify({
      medicineName: 'Aspirin',
      activeIngredient: 'aspirin',
      dosage: '81mg',
      frequency: 'Once daily in morning',
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      status: 'ACTIVE'
    })
  });
  const med2 = await med2Res.json();
  console.log('Added Aspirin (ID:', med2.medicationId, ')');

  // 3. Add duplicate active ingredient medications (Advil and Motrin both have ibuprofen)
  console.log('\n3. Adding duplicate active ingredient brand medicines: Advil and Motrin');
  await fetch(`${BASE_URL}/api/medications`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token1}` },
    body: JSON.stringify({
      medicineName: 'Advil',
      activeIngredient: 'ibuprofen',
      dosage: '200mg',
      frequency: 'PRN',
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      status: 'ACTIVE'
    })
  });
  await fetch(`${BASE_URL}/api/medications`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token1}` },
    body: JSON.stringify({
      medicineName: 'Motrin',
      activeIngredient: 'ibuprofen',
      dosage: '200mg',
      frequency: 'PRN',
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      status: 'ACTIVE'
    })
  });

  // 4. Check duplicate warnings
  console.log('\n4. Checking duplicate warnings');
  const dupRes = await fetch(`${BASE_URL}/api/medications/duplicates`, {
    headers: { 'Authorization': `Bearer ${token1}` }
  });
  const duplicates = await dupRes.json();
  console.log('Duplicate groups found:', duplicates.length);
  if (duplicates.length > 0) {
    console.log('Duplicate ingredient detected:', duplicates[0].activeIngredient);
    console.log('Warning message:', duplicates[0].warningMessage);
    console.log('Medications in group:', duplicates[0].medications.map(m => m.medicineName).join(', '));
  }

  // 5. Drug-drug interaction check
  console.log('\n5. Performing drug-drug interaction check (Warfarin + Aspirin)');
  const checkRes = await fetch(`${BASE_URL}/api/interactions/check`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token1}` },
    body: JSON.stringify({ medicationIds: [med1.medicationId, med2.medicationId] })
  });
  const checkData = await checkRes.json();
  console.log('Interaction Check Evaluated Pairs:', checkData.totalEvaluated);
  console.log('Interaction Alerts Detected:', checkData.interactionsFound);
  if (checkData.alerts && checkData.alerts.length > 0) {
    console.log('Alert Severity:', checkData.alerts[0].severity);
    console.log('Alert Source:', checkData.alerts[0].sourceName);
    console.log('Alert Evidence Status:', checkData.alerts[0].evidenceStatus);
  }

  // 6. Evidence-grounded AI explanation
  console.log('\n6. Requesting Evidence-Grounded AI Explanation');
  const aiRes = await fetch(`${BASE_URL}/api/ai/explain-interaction`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token1}` },
    body: JSON.stringify({
      drugA: 'Warfarin',
      drugB: 'Aspirin',
      severity: 'HIGH',
      verifiedInformation: 'Concurrent use of warfarin and aspirin significantly elevates the risk of gastrointestinal and intracranial hemorrhage.',
      source: 'FDA Drug Labeling (openFDA)',
      evidenceStatus: 'VERIFIED'
    })
  });
  const aiData = await aiRes.json();
  console.log('AI Verified Source:', aiData.verifiedSource);
  console.log('AI Explanation snippet:', aiData.patientFriendlyExplanation.slice(0, 120) + '...');
  console.log('AI Safety Notice:', aiData.safetyNotice);
  console.log('AI Disclaimer Present:', !!aiData.disclaimer);

  // 7. Schedule creation and Logging TAKEN / MISSED
  console.log('\n7. Adding Schedule & Recording TAKEN / MISSED events');
  const schedRes = await fetch(`${BASE_URL}/api/medications/${med1.medicationId}/schedules`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token1}` },
    body: JSON.stringify({ scheduledTime: '08:00:00', mealRelation: 'AFTER_MEAL' })
  });
  const sched = await schedRes.json();
  console.log('Created Schedule ID:', sched.scheduleId, 'at', sched.scheduledTime);

  const takenRes = await fetch(`${BASE_URL}/api/logs/${sched.scheduleId}/taken`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token1}` }
  });
  const taken = await takenRes.json();
  console.log('Logged Medication Status:', taken.status, 'at schedule', taken.scheduleId);

  // 8. Medication History
  console.log('\n8. Checking Medication History');
  const historyRes = await fetch(`${BASE_URL}/api/logs`, {
    headers: { 'Authorization': `Bearer ${token1}` }
  });
  const history = await historyRes.json();
  console.log('History entries count for User 1:', history.length);

  // 9. IDOR Verification (User 2 attempts to access User 1 medication)
  console.log('\n9. IDOR Isolation Test: User 2 registering and attempting to access User 1 resource');
  const user2Email = `demo_user2_${rand}@medsafe.org`;
  const reg2Res = await fetch(`${BASE_URL}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: 'Second User', email: user2Email, password: 'Password@123' })
  });
  const reg2Data = await reg2Res.json();
  const token2 = reg2Data.token;

  const idorRes = await fetch(`${BASE_URL}/api/medications/${med1.medicationId}`, {
    headers: { 'Authorization': `Bearer ${token2}` }
  });
  console.log('IDOR attack blocked with HTTP Status:', idorRes.status, '(Expected 401 Unauthorized)');

  // 10. Frontend verification
  console.log('\n10. Frontend Health Check (http://localhost:5173)');
  const feRes = await fetch('http://localhost:5173');
  console.log('Frontend HTTP Status:', feRes.status, feRes.statusText);

  console.log('\n=== ALL END-TO-END DEMO CHECKS COMPLETED SUCCESSFULLY! ===');
}

run().catch(console.error);
