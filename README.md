
# Anypoint Template: Salesforce to Database User Broadcast	

<!-- Header (start) -->

<!-- Header (end) -->

# License Agreement
This template is subject to the conditions of the <a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>. Review the terms of the license before downloading and using this template. You can use this template for free with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio. 
# Use Case
<!-- Use Case (start) -->
As a Salesforce admin I want to migrate Users from Salesforce to Database.

This template serves as a foundation for setting an online sync of users from one Salesforce instance to database. Every time there is a new User or a change in an already existing one, the integration will poll for changes in Salesforce source instance and it will be responsible for updating the User on the target database table.

What about Passwords? When the User is updated in the target instance, the password is not changed and therefore there is nothing to concern about in this case. Password set in case of User creation is not being covered by this template considering that many different approaches can be selected.

Requirements have been set not only to be used as examples, but also to establish a starting point to adapt your integration to your requirements.

As implemented, this template leverages the batch module.

The batch job is divided in *Process* and *On Complete* stages.

The integration is triggered by a scheduler defined in the flow that triggers the application, queries the newest Salesforce, updates or creates matching a filter criteria, and executes the batch job.

During the *Process* stage, each Salesforce user is filtered depending on if it has an existing matching user in the Database.
The last step of the *Process* stage groups the users and inserts or updates them in Database.

Finally during the *On Complete* stage the template logs output statistics data into the console.
<!-- Use Case (end) -->

# Considerations
<!-- Default Considerations (start) -->

<!-- Default Considerations (end) -->

<!-- Considerations (start) -->
To make this template run, there are certain preconditions that must be considered. All of them deal with the preparations in both source and destination systems, that must be made for the template to run smoothly. 
Failing to do so could lead to unexpected behavior of the template.
<!-- Considerations (end) -->

## DB Considerations

This template uses date time or timestamp fields from the database to do comparisons and take further actions. While the template handles the time zone by sending all such fields in a neutral time zone, it cannot handle time offsets. (Time offsets are time differences that may surface between date time and timestamp fields from different systems due to a differences in each system's internal clock.)
Take this in consideration and take the actions needed to avoid the time offset.


### As a Data Destination

**Note:** This template illustrates the migration use case between Salesforce and a Database, thus it requires a Database instance to work.
The template comes packaged with a SQL script to create the database table that it uses.
It is your responsibility to use the script to create the table in an available schema and change the configuration accordingly.
The SQL script file can be found in src/main/resources/user.sql.

This template is customized for MySQL. To use it with different SQL implementation, some changes are necessary:

* Update SQL script dialect to desired one
* Replace MySQL driver library dependency to desired one in pom.xml file
* Update Database Config to suitable connection instead of db:my-sql-connection in global elements in config.xml in /src/main/mule/.
* Update database properties in `mule.*.properties` file

## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work:

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>.
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>.

### As a Data Source

If the user who configured the template for the source system does not have at least *read only* permissions for the fields that are fetched, then an *InvalidFieldFault* API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault 
[ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='Account.Phone, Account.Rating, Account.RecordTypeId, 
Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to 
use a custom field, be sure to append the '__c' after the custom field name. 
Reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```
1. Users cannot be deleted in Salesforce: For now, the only thing to do regarding users removal is disabling/deactivating them, but this won't make the username available for a new user.
2. Each user needs to be associated to a Profile: Salesforce's profiles are what define the permissions the user has for manipulating data and other users. Each Salesforce account has its own profiles. In this kick you will find a processor labeled *assignProfileId and Username to the User* where to map your Profile Ids from the source account to the ones in the target account.
3. Working with sandboxes for the same account: Although each sandbox should be a completely different environment, Usernames cannot be repeated in different sandboxes, i.e. if you have a user with username *bob.dylan* in *sandbox A*, you will not be able to create another user with username *bob.dylan* in *sandbox B*. If you are indeed working with Sandboxes for the same Salesforce account you will need to map the source username to a different one in the target sandbox, for this purpose, please refer to the processor labeled *assign ProfileId and Username to the User*.










# Run it!
Simple steps to get this template running.
<!-- Run it (start) -->

<!-- Run it (end) -->

## Running On Premises
In this section we help you run this template on your computer.
<!-- Running on premise (start) -->

<!-- Running on premise (end) -->

### Where to Download Anypoint Studio and the Mule Runtime
If you are new to Mule, download this software:

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)

**Note:** Anypoint Studio requires JDK 8.
<!-- Where to download (start) -->

<!-- Where to download (end) -->

### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your Anypoint Platform credentials, search for the template, and click Open.
<!-- Importing into Studio (start) -->

<!-- Importing into Studio (end) -->

### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`.
+ Click `Mule Application (configure)`.
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`.
+ Click `Run`.
<!-- Running on Studio (start) -->

<!-- Running on Studio (end) -->

### Running on Mule Standalone
Update the properties in one of the property files, for example in mule.prod.properties, and run your app with a corresponding environment variable. In this example, use `mule.env=prod`. 
Once your app is all set and started, there is no need to do anything else. The application polls Salesforce to know if there are any newly created or updated objects and synchronize them.

## Running on CloudHub
When creating your application in CloudHub, go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the mule.env value.
<!-- Running on Cloudhub (start) -->
Follow other steps defined [here](#runonpremise) and once your app is all set and started, there is no need to do anything else. Every time a User is created or modified, it will be automatically synchronized to supplied database table as long as it has an Email.
<!-- Running on Cloudhub (end) -->

### Deploying a Template in CloudHub
In Studio, right click your project name in Package Explorer and select Anypoint Platform > Deploy on CloudHub.
<!-- Deploying on Cloudhub (start) -->

<!-- Deploying on Cloudhub (end) -->

## Properties to Configure
To use this template, configure properties such as credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
<!-- Application Configuration (start) -->
**Batch Aggregator Configuration**
+ page.size `1000`

**Scheduler Configuration**
+ scheduler.frequency `20000`
+ scheduler.start.delay `1000`

**Watermarking Default Last Query Timestamp**
+ watermark.default.expression `2016-12-13T03:00:59Z`
		
**Salesforce Connector Configuration**
+ sfdc.username `bob.dylan@org`
+ sfdc.password `DylanPassword123`
+ sfdc.securityToken `avsfwCUl7apQs56Xq2AKi3X`

**Database Connector Configuration**
+ db.host `localhost`
+ db.port `3306`
+ db.user `joan.baez`
+ db.password `JoanBaez456`
+ db.databasename `template-sfdc2db-user-broadcast`
<!-- Application Configuration (end) -->

# API Calls
<!-- API Calls (start) -->
Salesforce imposes limits on the number of API calls that can be made. Therefore calculating this amount may be an important factor to consider. User Broadcast Template calls to the API can be calculated using the formula:
***1 + X + X / ${page.size}***

***X*** is the number of Users to be synchronized on each run. 

Divide by ***${page.size}*** because by default, for each Upsert API call, Users are gathered in groups of a number defined by the ${page.size}. Also consider that this calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from origin instance, then 12 API calls will be made (1 + 10 + 1).
<!-- API Calls (end) -->

# Customize It!
This brief guide provides a high level understanding of how this template is built and how you can change it according to your needs. As Mule applications are based on XML files, this page describes the XML files used with this template. More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml<!-- Customize it (start) -->

<!-- Customize it (end) -->

## config.xml
<!-- Default Config XML (start) -->
This file provides the configuration for connectors and configuration properties. Only change this file to make core changes to the connector processing logic. Otherwise, all parameters that can be modified should instead be in a properties file, which is the recommended place to make changes.<!-- Default Config XML (end) -->

<!-- Config XML (start) -->

<!-- Config XML (end) -->

## businessLogic.xml
<!-- Default Business Logic XML (start) -->
Functional aspect of the template is implemented in this XML file, directed by one flow that polls for Salesforce creations/updates. 
For the purpose of this particular template the *mainFlow* uses a batch job, which handles all the logic of it.

The several message processors constitute four high level actions that fully implement the logic of this Template:
1. Firstly the template goes to the Salesforce and query all the existing users that match the filter criteria.
2. Then each Salesforce User is checked by email against database, if it has an existing matching user in database and then groups the users and inserts or updates them in Database.
3. Finally the template logs output statistics data in the console.<!-- Default Business Logic XML (end) -->

<!-- Business Logic XML (start) -->

<!-- Business Logic XML (end) -->

## endpoints.xml
<!-- Default Endpoints XML (start) -->
This is file is conformed by a Flow containing the Scheduler that periodically queries Salesforce for updated or created Users that meet the defined criteria in the query. And then executing the batch job process with the query results.<!-- Default Endpoints XML (end) -->

<!-- Endpoints XML (start) -->

<!-- Endpoints XML (end) -->

## errorHandling.xml
<!-- Default Error Handling XML (start) -->
This file handles how your integration reacts depending on the different exceptions. This file provides error handling that is referenced by the main flow in the business logic.<!-- Default Error Handling XML (end) -->

<!-- Error Handling XML (start) -->

<!-- Error Handling XML (end) -->

<!-- Extras (start) -->

<!-- Extras (end) -->
