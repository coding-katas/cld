# cliper-dedup



## Description
Service de deduplication kafka

## How it works on VMs

After installing Kafka cluster

```
cd cliper-producer
mvn clean package
mvn spring-boot:run

(another window)
cd cliper-dedup
mvn clean package
mvn spring-boot:run


(another window)
cd perfs
python3 dedup_perfs.py
```

## How it works with docker

```
cd docker_config
sudo docker-compose up -d
```

## Message Test

The message sent from MDM:
{"requestMessage":null,"responseMessage":null,"actionType":"add","entityId":"819668330144996909","entityType":"Person","messageCreationTime":1683301453656,
"controlParameter":[{"key":"request_ns","value":http://www.ibm.com/mdm/schema},{"key":"application_name","value":"tcrm"},{"key":"request_origin","value":"eCare"},{"key":"txnCategory","value":"add"},{"key":"client_system_name","value":"CLP"},{"key":"requester_locale","value":"fr"},{"key":"requester_lang","value":"200"},{"key":"request_name","value":"addParty"},{"key":"external_correlation_id","value":"test_cockpit_famile"},{"key":"RequestAdapter","value":"MDMOperationalServicesRequestAdapter"},{"key":"company","value":"Qualification"},{"key":"validated","value":"true"},{"key":"requester_name","value":"Injecteur Cliper - G3R4C0 - brng6193"},{"key":"customer_request_version","value":"G3R4C0"},{"key":"root","value":"true"},{"key":"userName","value":"Injecteur Cliper - G3R4C0 - brng6193"},{"key":"langId","value":"200"},{"key":"update_meth_code","value":"A1"},{"key":"locale_id","value":"fr"},{"key":"line_of_business","value":"Projet Cliper"},{"key":"service_call_identifier","value":"81"},{"key":"request_schema","value":"MDMDomains.xsd"},{"key":"txnId","value":"379068330144798409"}],"success":false}
# cliper-dedup
