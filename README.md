
# CorDapp Transfer of Care

Transfer of care cordapp is to digitize transfer of care between Hospital and social organizations.
Municipal council maintains medical records of all its citizens. When a patient is admitted in a hospital,
The hospital can get access to the patient’s medical records and can add additional information to it. When the hospital is done treating the patient they can discharge her. In case the patient needs further care, the hospital raises a ‘Transfer of care’ request with the Municipal Council which can review and approve\reject it.
In case of acceptance the status on the EHR should change accordingly to reflect the transfer of care and hospital should not be able to see or modify the EHR 
The cordapp allows you to admit a patient, update the patient’s medical records, discharge a patient, request transfer of care and Approve/Reject request for transfer of care

# Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

Instruction for setting up
To run the nodes:
https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp

Once the nodes are up, run gradlew process from root folder of project to start Hospital and Municipal servers in order to access API endpoints 
./gradlew runHospitalServer
./gradlew runMunicipalServer
API endpoints are available for nodes at:
•	Hospital http://localhost:10050

•	Municipal http://localhost:10055

Admit a Patient
The API endpoint is: http://localhost:10050/admit
`http://localhost:10050/admit?counterpartyName=Municipal&patientId=1&ehr=1&status=ADMITTED`

Update Medical records:
The API endpoint to update is: http://localhost:10050/update
`http://localhost:10050/update?patientId=1&event=has diabetes`

Discharge a patient:
The API endpoint for discharge is: http://localhost:10050/discharge
`http://localhost:10050/discharge?patientId=1&filePath=C:/Users/Administrator/Downloads/ehr1-medicalrecord.jar`

Request for transfer of care:
The API endpoint is: http://localhost:10050/request
`http://localhost:10050/request?patientId=1``

