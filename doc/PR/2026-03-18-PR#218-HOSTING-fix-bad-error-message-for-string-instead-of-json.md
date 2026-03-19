# PR#218: Bad error message for string instead of JSON

This PR contains an unrelated quickfix, see [Additional Changes](#additional-changes) below.

## The Problem

This request:

```
$ curl --no-progress-meter -X 'POST' \
'https://backend.ngdev.hostsharing.net/api/hs/booking/items' \
-H 'Accept: application/json' \
-H "Authorization: Bearer $BEARER" \
-H "Origin: http://127.0.0.1:3001" \
-H 'Content-Type: application/json' \
-d '{
  "project.uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "parentItem.uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "type": "PRIVATE_CLOUD",
  "caption": "string",
  "validTo": "2026-03-18",
  "resources": "string",
  "hostingAsset": {
    "parentAsset.uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "assignedToAsset.uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "type": "CLOUD_SERVER",
    "identifier": "string",
    "caption": "string",
    "alarmContact.uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "config": "string",
    "subHostingAssets": [
      {
        "type": "CLOUD_SERVER",
        "identifier": "string",
        "caption": "string",
        "assignedToAsset.uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "alarmContact.uuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "config": "string"
      }
    ]
  }
}' \
|jq|less

```

responses with this unclear error message:

```json
{
  "timestamp": "2026-03-18 03:59:56",
  "path": "",
  "statusCode": 500,
  "statusPhrase": "Internal Server Error",
  "message": "ERROR: [500] Map expected, but got: string"
}
```

## The Solution

Fortunately, in this case, it's a union (anyIf) at the API level (OpenAPI specification).
Thus, it cannot be parsed by the Spring Framework, but we parse it ourselves,
and I was able to simply pass the related property name and improve the error message.
