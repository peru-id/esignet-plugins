# peru-identity-plugin

## About
Implementation for all the interfaces defined in esignet-integration-api.

This library should be added as a runtime dependency to [esignet-service](https://github.com/mosip/esignet)

## Configurations required to be added / updated in esignet-default.properties

````
## Peru plugin configuration

mosip.esignet.integration.scan-base-package=io.peru.esignet.plugin
mosip.esignet.integration.authenticator=PeruAuthenticationService
mosip.esignet.integration.key-binder=PeruKeyBindingWrapperService
mosip.esignet.integration.vci-plugin=NoOpVCIssuancePlugin

## Peru KBA form Configuration 

mosip.esignet.peru.authenticator.auth-factor.kba.individual-id-field=dni
mosip.esignet.peru.authenticator.auth-factor.kba.field-details={{"id":"dni", "type":"text", "format":"", "maxLength": 10, "regex": "^[^\s]*$"},{"id":"prenombres", "type":"text", "format":"", "maxLength": 50, "regex": "^[^\s]*$"}\
  ,{"id":"primerApellido", "type":"text", "format":"", "maxLength": 50, "regex": "^[^\s]*$"},{"id":"segundoApellido", "type":"text", "format":"", "maxLength": 50, "regex": "^[^\s]*$"},{"id":"fechaNacimiento", "type":"date", "format":"dd/mm/yyyy"}}
mosip.esignet.authenticator.default.auth-factor.kba.field-details=${mosip.esignet.peru.authenticator.auth-factor.kba.field-details}
mosip.esignet.authenticator.default.auth-factor.kba.individual-id-field=${mosip.esignet.peru.authenticator.auth-factor.kba.individual-id-field}

## Peru mock OTP configuration
mosip.esignet.peru.authenticator.otp-value=111111

## Update below cache related configuration with "kycauth" cache name

mosip.esignet.cache.size={'clientdetails' : 200, 'preauth': 200, 'authenticated': 200, 'authcodegenerated': 200, 'userinfo': 200, \
   'linkcodegenerated' : 500, 'linked': 200 , 'linkedcode': 200, 'linkedauth' : 200 , 'consented' :200, 'vcissuance':100, \
  'apiRateLimit' : 500, 'blocked': 500,'kycauth': 500 }

mosip.esignet.cache.expire-in-seconds={'clientdetails' : 86400, 'preauth': 180, 'authenticated': ${mosip.esignet.authentication-expire-in-secs}, \
  'authcodegenerated': 60, 'userinfo': ${mosip.esignet.access-token-expire-seconds}, 'linkcodegenerated' : ${mosip.esignet.link-code-expire-in-secs}, \
  'linked': 60 , 'linkedcode': ${mosip.esignet.link-code-expire-in-secs}, 'linkedauth' : ${mosip.esignet.authentication-expire-in-secs}, \
  'consented': 120, 'vcissuance': ${mosip.esignet.access-token-expire-seconds}, 'apiRateLimit' : 180, 'blocked': 300, 'kycauth':1800}

mosip.esignet.cache.names=clientdetails,preauth,authenticated,authcodegenerated,userinfo,linkcodegenerated,linked,linkedcode,\
  linkedauth,consented,vcissuance,apiRateLimit,blocked,kycauth

## Peru identity endpoint configuration, update the API credentials based on the environment

identity.endpoint=http://localhost/consultadnie/ConsultaDniService
identity.endpoint.password=password
identity.endpoint.dni=api-dni
identity.endpoint.ruc=api-ruc

````

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
This integration plugin is compatible with [Sunbird-RC 1.0.0](https://github.com/Sunbird-RC/sunbird-rc-core/tree/v1.0.0)


