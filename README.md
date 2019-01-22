voa-property-linking
================

A microservice for the CCA (Business Rates) project at VOA. Allows access to property links, agent representations.

# Installation
### Cloning:

SSH
```
git@github.com:hmrc/voa-property-linking-frontend.git
```
HTTPS
```
https://github.com/hmrc/voa-property-linking-frontend.git
```
### Running the application

Ensure that you have the latest versions of the required services and that they are running. This can be done via service manager using the BUSINESS_RATES_ALL profile
```
sm --start PROPERTY_LINKING -f
sm --stop PROPERTY_LINKING 
```

* `cd` to the root of the project.
* `sbt run`

### Found a bug?

* Please raise an issue by selecting Issues near the top right hand side of this page.
* Add comments, logs and screenshots where possible. 

