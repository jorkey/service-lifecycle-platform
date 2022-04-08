<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Introduction](#introduction)
  - [Required platforms](#required-platforms)
  - [Agreements](#agreements)
  - [Services](#services)
    - [Service lifecycle](#service-lifecycle)
    - [Service profiles](#service-profiles)
    - [Service role](#service-role)
  - [Service versions](#service-versions)
    - [Developer version](#developer-version)
    - [Client version](#client-version)
    - [Desired versions](#desired-versions)
- [Services settings](#services-settings)
  - [Settings for updating and starting services](#settings-for-updating-and-starting-services)
  - [Service settings by the developer](#service-settings-by-the-developer)
  - [Customizing settings for the client](#customizing-settings-for-the-client)
  - [Private files](#private-files)
- [Services vm-update](#services-vm-update)
  - [Scripts](#scripts)
  - [Distribution Server](#distribution-server)
    - [Create distribution server](#create-distribution-server)
      - [Building and installing the distribution server from source codes](#building-and-installing-the-distribution-server-from-source-codes)
      - [Installing a distribution server from another distribution server](#installing-a-distribution-server-from-another-distribution-server)
    - [Distribution server setup](#distribution-server-setup)
      - [Configuration file](#configuration-file)
    - [Functions of distribution server](#functions-of-distribution-server)
      - [GraphQL queries](#graphql-queries)
      - [Logging](#logging)
      - [Tasks](#tasks)
      - [Services state handling](#services-state-handling)
      - [Handling fault reports](#handling-fault-reports)
      - [Interaction with other distribution servers](#interaction-with-other-distribution-servers)
    - [Update of distribution server](#update-of-distribution-server)
    - [Document history in Mongo DB](#document-history-in-mongo-db)
  - [Builder](#builder)
    - [Builder install and update](#builder-install-and-update)
    - [Distribution server install](#distribution-server-install)
    - [Build developer version](#build-developer-version)
    - [Build client version](#build-client-version)
  - [Updater](#updater)
    - [Install](#install)
    - [Configuration file updater.json](#configuration-file-updaterjson)
    - [Run updater](#run-updater)
    - [Updater execution](#updater-execution)
      - [Service install](#service-install)
      - [Start service](#start-service)
      - [Logging](#logging-1)
      - [Sending the status of services](#sending-the-status-of-services)
      - [Services failures handling](#services-failures-handling)
      - [Service abort by _updater_](#service-abort-by-_updater_)
      - [Service version update](#service-version-update)
    - [Self-update](#self-update)
- [Distribution Dashboard](#distribution-dashboard)
  - [Settings](#settings)
    - [Accounts](#accounts)
      - [Users](#users)
      - [Services](#services-1)
      - [Distribution Consumers](#distribution-consumers)
    - [Build](#build)
      - [Developer](#developer)
      - [Client](#client)
    - [Profiles](#profiles)
    - [Providers](#providers)
  - [Build developer version](#build-developer-version-1)
  - [Building client versions](#building-client-versions)
    - [With download from the provider](#with-download-from-the-provider)
    - [Without download from the provider](#without-download-from-the-provider)
    - [Monitoring the build process](#monitoring-the-build-process)
    - [Marking versions as tested](#marking-versions-as-tested)
  - [Desired versions](#desired-versions-1)
    - [Rollback to a previous revision of a list](#rollback-to-a-previous-revision-of-a-list)
    - [Assigning versions manually](#assigning-versions-manually)
  - [Monitoring logs](#monitoring-logs)
    - [Monitoring task logs](#monitoring-task-logs)
    - [Monitoring service logs](#monitoring-service-logs)
  - [Fault reports](#fault-reports)
  - [Dashboard](#dashboard)
    - [Panel Versions](#panel-versions)
    - [Last Developer Versions Panel](#last-developer-versions-panel)
    - [Last Client Versions Panel](#last-client-versions-panel)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

<a name="services_accounts"></a>
# Introduction

Vm-update performs versioning, updating, starting and monitoring of services.
History storage allows you to roll back changes.
Centralized storage of log journals and crash reports is convenient for 
monitoring system performance and troubleshooting errors.

## Required platforms

Currently, vm-update works on Linux Ubuntu servers.
Source codes must be in the Git VCS repositories.
Microsoft Azure cloud service is supported.

## Agreements

The following is accepted to display the structure of _Json_ files.

Pair name/value:
- _name_ : string
- _name:number_ : number
- _name:boolean_ : boolean

Object
- _name_
  - contents

Array
- _name[]_ - strings array
- _name:number[]_ - numbers array
- _name[]_ - objects array
  - ... - contents
  
Optional value:
- _[name]_

## Services

A service is an entire application, or a part of it.
For example, a service could be an executable file with a set of shared libraries,
or only shared libraries.

For each service, the assembly version and installation rules must be described.
If the service is an executable program, the startup rules are also described for it,
logging and crash reporting.

### Service lifecycle

- Creating a development version of the service
- Creation of a client version of the service with customization of settings for a specific client
- Installing/Upgrading a Service Instance
- Для выполнимых сервисов:
  - Run service instance
  - Writing service logs to database
  - Write service crash reports to database

### Service profiles

Profiles are named groups of services.
Profiles are used to designate distributed groups of services.

### Service role

The service can perform one of the assigned roles.
The role can be specified in the launch options or in the configuration file.

## Service versions

When creating a service, an archive of installation files and meta information about this archive are created,
marked with a unique version number.

### Developer version

Format of developer version is _distribution-build_:
- _distribution_ - the name of the distribution server on which the version was created.
- _build_ - build number from numbers separated by dots, for example *4.21.2*.

For the distribution server _jetto_ and build _3.5.1_, there will be a version _jetto-3.5.1_.

### Client version

The client version is created from the developer version on the distribution server
where it will be used.
The client version contains customization of the developer's version for a specific client.
It can specify specific settings, set keys, certificates, etc.

The initial client version has the same name as the developer version it was created with.
If the client settings have changed, the client version is recreated.
The new version will end with ._N, where N - client version generation number.
For example, created from the developer's version _jetto-3.5.1_,
the client version will be called _jetto-3.5.1_, next builds will be called 
_jetto-3.5.1_1_, _jetto-3.5.1_2_, etc.

### Desired versions

The distribution server stores a number of the latest generated versions of the service.

In order to indicate which version to use at the current moment, is intended
the list of desired versions in the format _service-version_.

There is a list of desired developer versions and desired client versions.

Desired versions are changed in a whole list, since the versions of the services depend on each other.

When generating a new version, by default, the new version number becomes desirable for the given service.
The desired version number can be changed manually.

# Services settings

<a name="update_json"></a>
## Settings for updating and starting services

The file _update.json_ is created in the main service development repository.
It describes the rules for building, installing and running.

File format:

- _update[]_ - service descriptions
  - _service_ : service name
  - _build_ - service version assembly rules
    - _[buildCommands[]]_ - version build commands
      - _command_ : build command
      - _[args[]]_ - build options
      - _[env[]]_ - command run environment
        - _'name'_ : value
      - _[directory]_ : directory to execute, if not specified, current
      - _[exitCode:number]_ : exit code
      - _[outputMatch]_ : matching output to stderr and stdout, regular expression
    - _copyFiles[]_ - files and directories to copy to assembly
      - _sourceFile_ : source file in the service repository
      - _destinationFile_ : file in version image
      - _except[]_ - exclude this files
      - _[settings[]]_ - macro values in source files. When copied to an assembly, 
                         the macro is replaced with a value.
        - _'macro name'_ : macro value
    - _install_
      - _[installCommands]_ : version install commands, as in _buildCommands_
      - _[postInstallCommands]_ : commands after installing the version, as in _buildCommands_
      - _[runService]_ - service start rules
        - _command_ : run command
        - _[args[]]_ - run arguments
        - _[writeLogs]_ - write service logs to local disk
          - _directory_ : directory to write logs
          - _filePrefix_ : start of log file name, full name _<filePrefix>.<number>_
          - _maxFileSizeMB_ : maximum log file size
          - _maxFilesCount_ : maximum number of log files
        - _[uploadLogs:boolean]_ - uploading service logs to the update server
        - _[faultFilesMatch]_ - regular expression to search for files with crash information
          when the service fails unexpectedly. The files are included in the crash report.
        - _[restartOnFault:boolean]_ - restart service after fault
        - _[restartConditions]_ - conditions for service restart
          - _[maxMemoryMB:number]_ : the service used more than the specified number of megabytes of memory
          - _[maxCpu]_ - the service used more CPU resources than specified
            - _percents:number_ : percentage of CPU usage
            - _durationSec:number_ : within the specified time interval
          - _[makeCore:boolean]_ : for Unix-like systems, service termination with SIGQUIT signal,
            _core dump_ is created by this signal
          - _checkTimeoutMs:number_ : interval between check conditions

Macros may be present in the string values of the settings:
- _%%version%%_ - Developer version
- _%%role%%_ - Service role. Can be specified in parameters of commands: _installCommands_, 
               _postInstallCommands_ and _runService_.

When generating the developer version, the _install_ section from the _update.json_ file is written to the _install.json_ file
version image root directory.

## Service settings by the developer

Common settings for the service, which are determined by the developer, are placed in the source files.
In common settings defined default values, or macros `%%name%%`.
Common settings are recorded to developer version image.

<a name="settings_customization"></a>
## Customizing settings for the client

When generating a client version, the settings from the developer may be defined, supplemented and overridden.
To do this, a directory with additional settings is created, with the same structure as the version image directory.

The complement of common settings by optional settings done according to the following rules:
- If it is required to complete the settings of a file with the extension _.conf_ (typesafe format), _.json_ or _.properties_,
a file is created with the same name and path to this file. This file adds or redefines
  settings section. The content of the files will be automatically merged.
- If you need to define macro values `%%name%%` in the settings file, a file with the same name and extension _.defines_ is created, 
  which contains values for macro definitions in the format `name=value`.
- For one configuration file, several additional files can be created with
  adding an index to the name _.<index>_. Files are applied sequentially, according to the index.
  This allows you to change the client configuration from different sources.
- If you just want to add a settings file, an additional file with a unique name is created.

The _install.json_ file can be completed for the client according to the same rules.

## Private files

In addition to source codes, sometimes it is required to include to the image of service version of files with private information,
such as access keys. This is often needed for client versions.
Such files are not stored in source repositories, but are uploaded to the distribution server directly.

# Services vm-update

VM-update itself consists of services. Thus, vm-update creates versions of itself and updates itself.

Services vm-update:
- scripts - Startup and initializing scripts (Shell, YAML)
- distribution - Distribution server
  - Backend
    - Distribution Web server [Scala Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html)
      - Supports GraphQL requests [Sangria GraphQL](https://github.com/sangria-graphql/sangria)
    - Data base [MongoDB](https://github.com/mongodb/mongo)
  - Frontend
    - Distribution Dashboard [React Js](https://reactjs.org)
- builder - Version builder (Scala command line application)
- updater - Services installer and executor (Scala command line application)

## Scripts

Perform initial installation and service upgrades.

- _.update.sh_ - base script, updates the scripts themselves, starts and updates the service
  - _distribution_ - distribution server scripts
    - _.create_distribution_service.sh_ - installs the distribution server as an operating system service,
                                          only Linux systemd is currently supported 
    - _.make_distribution_config.sh_ - creates an initial configuration file
    - _distribution.sh_ - start distribution server
  - _builder_ - _builder_ scripts
    - _builder.sh_ - run _builder_
  - _updater_ - _updater_ scripts
    - _.updater_setup.sh_ - installs _updater_
      - creates _updater_ configuration file
      - installs _updater_ as operation system service,
        currently only Linux systemd is supported
    - _instance.yaml_ - start script of _cloud-init_ 
    - _updater.sh_ - runs _updater_

## Distribution Server

The main component of vm-update is the distribution server, managed through the Distribution Dashboard.
New versions of services are created on the distribution server. To create a new version, _distribution_ starts _builder_.

<a name="create_distribution_server"></a>
### Create distribution server

The distribution server can be built and installed from source, or from another distribution server.

#### Building and installing the distribution server from source codes

Install data base MongoDB.

Clone source repository vm-update to distribution server:

`git clone git@github.com:jorkey/vm-update.git`

Execute the command in the repository directory:

`sbt "project builder; run buildProviderDistribution [cloudProvider=?] distribution=? directory=? host=? port=? [sslKeyStoreFile=?] [sslKeyStorePassword=?] title=? mongoDbConnection=? mongoDbName=? [persistent=?]"`

Replace `?` with values:
- _cloudProvider_
  - type of cloud service, if the distribution server is in the cloud:
      - Azure - the only currently supported cloud
- _distribution_
  - unique distribution server name, no spaces
- _directory_
  - installation directory
- _host_
  - DNS name for the service
- _port_
  - TCP port
- _[sslKeyStoreFile]_
  - file with private key and certificate for HTTPS service
- _[sslKeyStorePassword]_
  - password to file with private key and certificate for HTTPS service
- _title_
  - title of distribution server
- _mongoDbConnection_
  - MongoDB connection URL, for example mongodb://localhost:27017
- _mongoDbName_
  - MongoDB database name
- _persistent_
  - currently only for installation on Linux
  - _true_, if the distribution server is installed as a Linux systemd service 

After the command completes, the distribution server process should start.

#### Installing a distribution server from another distribution server

Create an account for the new _customer_ on the source distribution server (see [Service accounts](#services_accounts)).

Install the MongoDB database.

Copy the file _.update.sh_ to the temporary directory from the directory of the source distribution server.
Create the script _clone_distribution.sh_ in the same place:

```
#!/bin/bash -e
set -e

serviceToRun=builder

distributionUrl=?
accessToken=?

. .update.sh buildConsumerDistribution providerUrl=? [cloudProvider=?] distribution=? directory=? host=? port=? [sslKeyStoreFile=?] [sslKeyStorePassword=?] title=? mongoDbConnection=? mongoDbName=? provider=? consumerAccessToken=? testConsumerMatch=? [persistent=?] 
```

Replace `?` with values:
- _providerUrl_
  - source distribution server URL
- _cloudProvider_
  - type of cloud service, if the distribution server is in the cloud
    - Azure - the only currently supported cloud
- _distribution_
  - distribution server unique name
- _directory_
  - installation directory
- _host_
  - DNS name for the service
- _port_
  - TCP port
- _[sslKeyStoreFile]_
  - file with private key and certificate for HTTPS service
- _[sslKeyStorePassword]_
  - password to file with private key and certificate for HTTPS service
- _title_
  - title of distribution server
- _mongoDbConnection_
  - MongoDB connection URL, for example mongodb://localhost:27017
- _mongoDbName_
  - MongoDB database name
- _provider_
  - source distribution server name
- _consumerAccessToken_
  - Access Token for _consumer_ account
    - Go to _Settings/Accounts/Distribution Consumers_ and click on the key icon for the relevant entry.
- _testConsumerMatch_
  - test installation distribution server name
- _persistent_
  - currently only for installation on Linux
  - _true_, if the distribution server is installed as a Linux systemd service

Run script _clone_distribution.sh_.
After the script completes, the distribution server process will start.

### Distribution server setup

#### Configuration file

The _distribution.json_ configuration file is located in the working directory of the distribution server and has the following format:

- _distribution_ : distribution server unique name
- _title_ : distribution server header
- _instance_ : virtual machine instance identifier
- _jwtSecret_ : encoding key JWT
- _mongoDb_ - setting up a connection to Mongo DB
  - _connection_ : URL подключения
  - _name_ : base name
- _network_
  - _port:number_ : HTTP or HTTPS server port number
  - _[ssl]_ - settings for HTTPS service
    - _keyStoreFile_ : JKS file with keys and certificates
    - _keyStorePassword_ : key store access password
- _versions_ - version history
  - _maxHistorySize:number_ : maximum number of service versions in history
- _serviceStates_ - state of services
  - _expirationTimeout:FiniteDuration_ : duration of storage of the last state of the service.
    If the state is not updated during this time, the service instance is considered dead.
- _logs_ - logs of services
  - _serviceLogExpirationTimeout:FiniteDuration_ : duration of service log record storage in the database
  - _taskLogExpirationTimeout:FiniteDuration_ : duration of task log record storage in the database
- _faultReports_ - fault reports
  - _expirationTimeout:FiniteDuration_ : duration of fault report storage in the database
  - _maxReportsCount:number_ : maximum number of fault reports in the database

_FiniteDuration_ format:
- unit - unit of time: "SECONDS", "HOURS", "MINUTES", "DAYS"
- length:number - number of units
  
### Functions of distribution server

#### GraphQL queries

The distribution server accepts GraphQL requests from users, other vm-update services, and other distribution servers.
GraphQL queries serve:
  - accounts
  - distribution servers connection settings
  - source code repositories
  - profile of services
  - developer versions
  - desired developer versions
  - client versions
  - desired client versions
  - store/find state of services
  - store/find logs of services
  - store/find fault reports

To build versions, the distribution server runs _builder_ locally or on another distribution server.

#### Logging

Service logs, including logs of _distribution_ itself, are written to a MongoDB collection.
Each entry in the collection contains one log entry and consists of the following fields:
- _service_ - service name
- _instance_ - instance identifier of the server on which the service is running 
- _directory_ - current process directory
- _process_ - process identifier
- _[task]_ -  the task identifier, if the entry was received while the task was running
- _time:Date_ - time and data of log record
- _level_ - debug level
- _unit_ - functional unit
- _message_ - lgo message
- _terminationStatus:Boolean_ - termination status
  - true - success
  - false - error 

The GraphQL schema has a query to search for log entries by different criteria.
It is also possible to do a GraphQL _subscription_ on the logs, and then new records in the log will be
flow to the GraphQL client in real time.

#### Tasks

Actions asynchronous to GraphQL queries are framed as tasks.
Task information is written to the MongoDB collection in the following format:

Actions asynchronous to GraphQL queries are executed in tasks.
Task information is written to a MongoDB collection in the following format:
- _id_ - task identifier
- _type_ - task type
- _parameters_ - set of parameters, in the format:
  - _name_ - parameter name
  - _value_ - parameter value
- _creationTime:Date_ - time and data when task is created

The execution log and task completion status can be retrieved from the logs.

<a name="service_state"></a>
#### Services state handling

_Updater_ periodically sends to distribution server status of running services.
The state of the services is written to the MongoDB collection, and to the local disk.

State record format:
- _distribution_ - identified of distribution server
- _instance_ - instance identifier
- _service_ - service name
- _directory_ - service working directory
- _state:ServiceState_ - service state
  - _time_ - time of state
  - _[installTime]_ - installation time
  - _[startTime]_ - service start time
  - _[version]_ - current version of service
  - _[updateToVersion]_ - target version, if the service is currently being updated
  - _[updateError]_ - update error
  - _[failuresCount]_ - count of service failures after install and run _updater_
  - _[lastExitCode]_ - last exit code

#### Handling fault reports

Fault reports are store to MongoDB collection, and to local file storage.

Document format:
- _distribution_ - identifier of distribution server 
- _id_ - identifier of fault report
- _info_ - last service state
  - _time_ - time of state
  - _instance_ - identifier of instance
  - _service_ - service name
  - _[serviceRole]_ - service role
  - _serviceDirectory_ - working directory of service 
  - _serviceState:ServiceState_ - service state
  - _logTail:LogLine[]_ - last 500 log records
- _files[]_ - files with fault information

#### Interaction with other distribution servers

Distribution servers can be networked on a provider/consumer basis.

The provider profile defines the services for the consumer.
If the provider has new versions, the consumer can download and install them.
Thus, a company developing services can create a consumer account
on its distribution server and determine the list of services that it wants to give to it.

The consumer periodically sends information to the provider about the provider's installed services:
- Installed versions numbers
- Instances state
- Fault reports

The provider can also execute _builder_ for the consumer.
This is necessary if the consumer server does not have sufficient resources to build the version, or the provider
has the necessary set of system libraries.

It is possible to automatically update the services of the consumer when appeared new developer versions at the provider.
The consumer periodically checks for updates, and when updates are found, runs an update task.

### Update of distribution server

The distribution server is a vm-update service, which means that it is built and updated by general rules.
The server periodically checks its current version against the client version in the database,
and if they differ, restarts. Scripts after the completion of the distribution server install and run
new version.

The distribution server also terminates if the client version of the scripts has been updated.
In this case, the scripts update themselves and start the distribution server again.

### Document history in Mongo DB

All vm-update documents are stored in collections of a special format.

Each document, except for the content part, has additional fields:
- __sequence_ - sequence number of adding/modifying the document in the collection
  - indexed by ascending and descending
- __modifyTime_ - time of adding/modifying the document
- __archiveTime_ - document deletion time
  - indexed by ascending with option expire after 7 days

Adding custom fields allows to:
- Sort documents in collections by the order they were added/modified
- Store the last modification date of a document
- Keep a history of deleted documents for 7 days, and restore deleted document if necessary

## Builder

Produces:
- Build/install distribution server
- Build developer versions
- Build client versions

### Builder install and update

Builder is installed by the distribution server in the _<distributionDir>_/builder/_<distribution>_ directory.
Here:
- _distributionDir_ - distribution server working directory
- _distribution_ - distribution server name

Installation and updating is performed by vm-update scripts from the distribution server for which the assembly is performed.

### Distribution server install

Has been described in the section (см [Create distribution server](#create_distribution_server)).

<a name="build_developer_version"></a>
### Build developer version

The distribution server executes the command:

`builder.sh buildDeveloperVersion service=? version=? sourceRepositories=? [macroValues=?] [buildClientVersion=true/false] comment=?`

Here:
- _service_ - service for which the version is being produced
- _version_ - number of new version
- _sourceRepositories_ - source repositories configuration, Json format:
  - []
    - _name_ - repository name, is the name of the directory where the sources are copied
    - _git_
      - _url_ - Git repository access URL
      - _branch_ - Git repository branch
      - _[cloneSubmodules]_ - clone submodules
      - _[subDirectory]_ - subdirectory inside repository
                           If specified, sources are taken only from this directory.
- _privateFiles[]_ - list of private files to include in version, array in Json format
- _[macroValues]_ - macro values in settings in Json format:
  - []
    - _name_ - name
    - _value_ - value
- _comment_ - comment to the new version

Version build steps:
- _clone_ or _pull_ repositories are produced in the `<builderDir>/developer/services/<service>/source` directory,
  specified in _sourceRepositories_. Each repository is copied into a subdirectory called _name_ from the configuration.
- The version is built in the first directory described in _sourceRepositories_. This directory should contain
  the _update.json_ file described above. A link to another directory from another repository should look like `../<name>`.
- When building, commands from _build/buildCommands_ are executed. If macros are present in the description of commands, macro substitutions will be made:
  - macro _%%version%%_ is replaced with the build version
  - other values are taken from the _values_ parameter
- If _exitCode_ is given, its value is compared with the command's return code.
- If _outputMatch_ is set, the output of the command must match this regular expression.
- After executing the build commands, the files specified in _copyFiles_ are copied from directory 
`<builderDir>/developer/services/<service>/source` to directory `<builderDir>/developer/services/<service>/build`
- The _install_ section of the update.json file is written to the install.json file of the build directory.
- The _privateFiles_ files are downloaded from the distribution server and written to the build directory.
- Packing the build directory to ZIP file and downloading the version image and version information
  to the distribution server.
- Then the source file repositories are tagged with the generated version number.
Tag format: `<service>-<distribution>-<build>`.

<a name="build_client_version"></a>
### Build client version

Distribution server runs command:

`builder.sh buildClientVersion service=? developerVersion=? clientVersion=? [settingsRepositories=?] [macroValues=?]`

Where:
- _service_ - service for which the version is being produced
- _developerVersion_ - source developer version from which the client version is made
- _clientVersion_ - client version
- _[settingsRepositories]_ - developer version with which the client version is made:
  - []
    - _name_ - repository name, is the name of the directory where the settings are copied
    - _git_
      - _url_ - Git repository access URL
      - _branch_ - branch of Git repository
      - _[cloneSubmodules]_ - clone with с submodules
    - _[subDirectory]_ - directory name inside the repository
      If specified, settings are taken from this directory only
- _privateFiles[]_ - list of private files to include to the version, array in Json format
- _[macroValues]_ - macro values in settings in Json format as in _buildDeveloperVersion_

Build version steps:
- The version image is downloaded from the distribution server and unpacked 
  to directory `<builderDir>/client/services/<service>/build`.
- Produces clone of repositories, specified in _settingsRepositories_ to the 
  directory `<builderDir>/client/services/<service>/settings`.
  Each repository is copied into a subdirectory called _name_ from the configuration.
- Additional settings are merged with developer version settings 
  (see [Customing settings for client](#settings_customization))
- The _privateFiles_ files are downloaded from the distribution server and written to the build directory.
- The build directory is packaged into a ZIP file, which is uploaded to the distribution server
  along with version information.

## Updater

Installs, runs and updates service instances.

### Install

Produces by script _.updater_setup.sh_. 
Script creates configuration file _updater.json_ and installs _updater_ as service _systemd_.

`.updater_setup.sh <cloudProvider> <name> <services> <distributionUrl> <accessToken> [environment]`

Where:
- _cloudProvider_ 
  - Name of cloud service. The only value currently supported is _Azure_.
- _name_
  - Service name part _systemd_. The full name will be `update-${name}.service`. 
- _services_
  - Services to run, separated by commas, can be added to the service name
    the role it runs as `-<role>`
- _distributionUrl_
  - server distribution URL
- _accessToken_
  - Access token for access to distribution server

### Configuration file updater.json

- instanceId
  - VM instance identifier
- distributionUrl
  - server distribution URL
- accessToken
  - Access token for access to distribution server

### Run updater

`updater.sh runServices services=<service1>[-<role>][,...]`

Where:
- services - services for run, as for _.updater_setup.sh_ 

### Updater execution

For each of the services specified in the arguments, _updater_ does the following.

#### Service install

The desired client version for this service is downloaded from the distribution server and unpacked
to the build directory _<service[-role]>/new_.

The _install.json_ file is read from the build directory.
In the build directory, _installCommands_ commands are executed sequentially with pre-executed macro extensions:
- _role_ - service role
- _version_ - developer version

Build directory _<service[-role]>/new_ renamed to work directory _<service[-role]>/current_.

In the working directory, the _postInstallCommands_ commands are executed sequentially with pre-executed macro expansions:
- _role_ - service role
- _version_ - developer version

The installation commands are divided into two groups to reduce the time the service is inactive during an upgrade.
The _installCommands_ group contains commands that can be executed from any directory.
They are executed until the old version is stopped.
The _postInstallCommands_ group contains commands that can only be executed from the working directory.

#### Start service

In the _<service[-role]>/current_ directory, the _runService_ command is executed
with pre-made macro expansions:
- _role_ - service role
- _version_ - developer version

#### Logging

The service should write the operation log to standard output. The log line format should be:
`date level unit message`

Where:
_date_ - date in format `yyyy-MM-dd HH:mm:ss.SSS`
_level_ - logging level: _TRACE_, _DEBUG_, _INFO_, _WARN_ или _ERROR_
_unit_ - functional unit
_message_ - logging message

The service's standard output and error output are grabbed by _updater_.

If the _writeLogs_ section is defined in the configuration, the received work log lines are written
to the _<service[-role]>/current/<directory>_ directory according to the specified rules.

If the _uploadLogs_ parameter is set to `true`, log lines are also sent to the distribution server
in real time.

#### Sending the status of services

Once every 10 seconds, _updater_ sends the state of services _ServiceState_ to the distribution server
(see [Services state handling](#service_state)). 

#### Services failures handling

If the service terminates unexpectedly, a crash report is generated.

The regular expression contained in the _faultFilesMatch_ setting describes files with fault information
to be placed in fault report. For example, it could be a _core dump_ file, or Java hprof file.

The fault report is written to the directory <service[-role]>/faults/<time>.
The fault report is archived and sent to the distribution server.

If the setting value _restartOnFault_ is not defined, or is defined as _true_, the service is started again.

#### Service abort by _updater_

The _restartConditions_ setting describes the conditions for forcibly terminating the service.

To limit the service by the amount of memory used, the _maxMemoryMB_ parameter is specified.
If the service process exceeds the specified limit, the service is forcibly terminated.

To limit the service by CPU usage, the _maxCpu_ parameter is specified.
If the service process uses more than the specified _percents_ during _durationSec_,
the service is forcibly terminated.

The _makeCore_ parameter indicates that the service should be terminated with a SIGQUIT signal,
to form a _core dump_.

The interval for checking the occurrence of service interruption conditions is set by the _checkTimeoutMs_ parameter.

#### Service version update

If the desired version of the service changes on the distribution server, updater:
- downloads new version
- executes commands from _installCommands_
- stops old version
  - sends stop signal to service process and its descendants
- service logs are copied to the directory <service[-role]>/log.history/<version>-<time>.log>
- executes commands from _postInstallCommands_
- runs new version

### Self-update

If the desired version of _updater_ or _scripts_ changes on the distribution server, _updater_ downloads
new versions, they are not updated immediately, but waits until one of the updater's services is updated.

As soon as the desired version of one of the services changes on the distribution server, _updater_ downloads updates
and restarts.

# Distribution Dashboard

## Settings

Open the link to the distribution server in the browser.
Enter Account Name: admin and Password: admin.
You are logged in as an administrator.

Change the initial password. 
To do this, go to _Settings/Accounts/Users_, select the user _admin_, and click "Change Password".

### Accounts

Administered in _Settings/Accounts_.

#### Users

Register Dashboard developers and administrators.

<a name="services_accounts"></a>
#### Services

Register service accounts. After installing the distribution server are already present _builder_ and _updater_ entries.

By clicking on the key icon, you can get an Access Key for this service.
The Access Key is used in service startup and update scripts.

By clicking on the key icon, you can get an Access Key for this service.
The Access Key is used in service startup and update scripts.

#### Distribution Consumers

Accounts of other distribution servers.
The account includes, among other things:
- Distribution server URL. It is used by _builder_ to build the service remotely.
- Services Profile - a list of developed services that are supplied to this distribution server.

### Build

Version build settings.

#### Developer

Settings for the development version build.
Includes:
- Setting distribution server to run _builder_
- Description of parameters for run _builder_:
  - Сервер дистрибуции для запуска builder 
  - Environment
  - Source code repositories, macro values
    (see [Build developer version](#build_developer_version))
  - Private files
    - Path - the path to the file in the build directory
    - File To Upload - local file to download

#### Client

Settings for building the client version.
Includes:
- Setting the distribution server to run _builder_
- Description of parameters for launching _builder_:
  - Distribution server to run builder
  - Environment
  - Settings repositories, macro values
    (see [Build client version](#build_client_version))
  - Private files
    - Path - the path to the file in the build directory
    - File To Upload - local file to download

### Profiles

Service profiles are subsets of the list of development services.
Next, the profile is assigned to _distribution consumer_ in the settings.

To refer to services in development for which client versions are also being created
on a given distribution server, the _self_ profile is defined.

### Providers

This defines the providers of this distribution server.
Among other things, for the provider are determined:
- _Access Token_ - Access key for requests to the server
- _Test Consumer_ - The name of the consumer, after testing which, services can be installed on this server 
- _Upload State Interval_ - State load interval per provider
- _Auto Update_ - When you change the desired versions at the provider, they are automatically
                  downloaded and installed on the distribution server. The interval for checking is 10 sec.

## Build developer version

After the development services are defined, open _Build/Developer_.
A table of services and their versions will appear:
- Service - service under development
- Last Version, Author, Time, Comment - latest version, author and generation time of the service
- Status
  - In Process - version is currently being built
  - Completed - build completed

If the service version is not currently being built, you can run the task
of creating a new version by selecting an entry.
The new version number will be one more than the old one, or it can be assigned manually.
If this service is present in the 'own' client services profile, by default
you will be prompted to create a client version as well.

Clicking on _"Get Lat Commit Comment"_ will read the change history of the service repositories.
The _Comment_ field will display the comment for the last commit in the service repository.

After starting the new version creation task, the task execution log will be displayed
in real time. You can interrupt the version creation task.

If a version is currently being created, by selecting an entry, you can view the creation log in
real time. You can interrupt the version creation task.

## Building client versions

Open _Build/Client_ if development services are defined, otherwise _Build_.

If client versions are not currently being built, a build start dialog will appear.

### With download from the provider

If a provider is selected, a table of that provider's services will appear, in relation to the consumer's profile:
- Service - service for installation
- Provider Desired Version - the desired version of the service from the provider
- Developer Desired Version - desired developer version on the distribution server
- Client Desired Version - desired client version
- Tested Developer Version - last tested version

There are already selected services for building for which there are no client versions with the desired version from the provider.
Other services cannot be selected for assembly.

### Without download from the provider

If no provider is selected, the native services described in the 'own' profile are displayed:
- Service - service for installation to client 
- Developer Desired Version - desired developer version on the distribution server
- Client Desired Version - desired client version

There are already selected services for assembly for which there are no client versions
with the desired developer version on the distribution server.

You can also mark for assembly services for which client versions have already been built.
This makes sense if the client configuration has changed and you need to rebuild the client version.
In this case, a client version will be generated with a new client version build number.

### Monitoring the build process

After the build is run, a real-time task execution log will be displayed.
If necessary, you can interrupt the build task.

The task execution log is also displayed if, when opening the build client versions dialog
assembly is already in progress.

### Marking versions as tested

Before installing services on production servers, it is advisable to conduct comprehensive testing.
In order for services to be installed on working servers only after they are
tested, vm-update has a mechanism to mark the list of service versions as tested.

For example, there is a distribution server named _production_,
in the configuration of which the test distribution server _test_ is specified.
Installation of new versions of services on the _production_ server will occur only
if they have been tested on _test_ and marked as tested by _test_.

## Desired versions

In this page, you can edit the lists of desired developer and desired client versions.
For example, you can roll back an update, or several recent updates.
This is possible because the distribution server stores the last few installed versions and history
changes to desired versions.
Editing is done in one of two ways.

### Rollback to a previous revision of a list

To move to an older revision, press the 'down' button, to move to a newer revision, press the 'up' button.
The list revision time and author are listed at the top. 
The entries in the table display the desired versions of the selected revision compared to the latest revision:
- unmodified records are displayed in standard font
- bold - services for which the desired version has changed
- red - in this revision there is no service that is in the latest revision
- green - in this edition there is a service that is not in the latest edition

To make the list of desired versions current, click _Save_.

### Assigning versions manually

You can also arbitrarily assign versions to services. To do this, click on the service version,
after which a list of available versions in the history will appear.
When the version is changed, the record with the service is highlighted in bold.

To make the edited list of desired versions the current one, click _Save_.

## Monitoring logs

You can view logs in the context of a task or service execution.

### Monitoring task logs

The task table is displayed in reverse chronological order:
- Creation Time - task creation time
- ID - task identifier
- Type - task type 
- Parameters - task parameters with format: _name:value_
- Active - task is currently running

When a task is selected, entries in the task log are displayed:
- Time - logging time
- Level - log level
- Line - record

You can select logs by:
- Level - log level
  - when the level is selected, records of this level and above are displayed
- Find Text - sample text

When _Follow_ is selected, the end of the log is displayed, new log entries are displayed in real time.

### Monitoring service logs

The logs of the selected service are searched with an optional selection by:
- Instance - instance identifier
- Directory - work directory of service
- Process - service process number
- Level - log level
  - when the level is selected, records of this level and above are displayed
- From/To - time range
- Find Text - sample text

When _Follow_ is selected, the end of the log is displayed, new log entries are displayed in real time.

## Fault reports

The distribution server receives reports about failures of its services, and the services of its consumers.
By default, all reports are displayed in descending chronological order.
You can narrow search by the following criteria:
- Distribution - distribution server
- Service - service name
- From/To - time range

When you select a report, information about the failure and the latest entries of the service log are displayed.

It is possible to download the ZIP archive of fault report to a file.

## Dashboard

The _Dashboard_ page is intended for operational control of _vm-update_.
With its, you can see how _vm-update_ lives, quickly find possible problems,
rollback installed versions.

Panels are used to display information from different angles.

### Panel Versions

Designed to monitor the distribution and operation of service versions.

A table with fields is displayed:
- Service - service name
- Developer Desired Version
- Client Desired Version
- Working Version - client version currently running
- Directory - service working directory
- Instances - instances identifiers list
- Info - service state

Thus, this table entry displays the full distribution path of the service version from
developer version to the version running on the instance and its state.

If the distribution path is completed completely, all three versions will be the same.
Otherwise, the first version that does not match in the path is displayed in red.
When _Only Alerts_ is selected, only services with an incomplete distribution path are displayed.

When a consumer is selected, the versions installed on that consumer are displayed.

### Last Developer Versions Panel

The latest released and currently realising versions of the developer are displayed.

### Last Client Versions Panel

The latest released and currently realising versions of the client are displayed.


