<a name="services_accounts"></a>
# Introduction

V-update performs versioning, updating, starting and monitoring of services.
History storage allows you to roll back changes. 
Centralized storage of log journals and crash reports is convenient for 
monitoring system performance and troubleshooting errors.

## Required platforms

Currently v-update works on Linux Ubuntu servers.
Source codes must be in the Git VCS repositories.
Work with Microsoft Azure cloud service is supported.

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

# Services v-update

V-update itself consists of services. Thus, v-update creates versions of itself and updates itself.

Services v-update:
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

The main component of v-update is the distribution server, managed through the Distribution Dashboard.
New versions of services are created on the distribution server. To create a new version, _distribution_ starts _builder_.

<a name="create_distribution_server"></a>
### Create distribution server

The distribution server can be built and installed from source, or from another distribution server.

#### Building and installing the distribution server from source codes

Install data base MongoDB.

Clone source repository v-update to distribution server:

`git clone git@github.com:jorkey/v-update.git`

Выполните команду в каталоге репозитория:

`sbt "project builder; run buildProviderDistribution [cloudProvider=?] distribution=? directory=? host=? port=? [sslKeyStoreFile=?] [sslKeyStorePassword=?] title=? mongoDbConnection=? mongoDbName=? [persistent=?]"`

Вместо `?` подставьте значения:
- _cloudProvider_
  - тип облачного сервиса, если сервер дистрибуции находится в облаке:
      - Azure - единственное на данный момент поддерживаемое облако
- _distribution_
  - уникальное имя сервера дистрибуции, без пробелов
- _directory_
  - каталог для установки
- _host_
  - DNS имя для сервиса
- _port_
  - порт для сервиса
- _[sslKeyStoreFile]_
  - файл с приватным ключом и сертификатом для сервиса https
- _[sslKeyStorePassword]_
  - пароль к файлу с приватным ключом и сертификатом для сервиса https
- _title_
  - короткое описание сервера дистрибуции
- _mongoDbConnection_
  - URL подсоединения к MongoDB, например mongodb://localhost:27017
- _mongoDbName_
  - имя базы данных MongoDB
- _persistent_
  - в настоящее время только для установки на Linux. 
  - _true_, если сервер дистрибуции устанавливается как сервис systemd Linux. 

После завершения команды должен запуститься процесс сервера дострибуции.

#### Установка сервера дистрибуции с другого сервера дистрибуции

Заведите учётную запись для нового _customer_ на исходном сервере дистрибуции (см [Учётные записи сервисов](#services_accounts)).

Установите базу данных MongoDB.

Скопируйте во временный каталог из каталога исходного сервера дистрибуции файл _.update.sh_.
Создайте там же скрипт _clone_distribution.sh_:

```
#!/bin/bash -e
set -e

serviceToRun=builder

distributionUrl=?
accessToken=?

. .update.sh buildConsumerDistribution providerUrl=$distributionUrl [cloudProvider=?] distribution=? directory=? host=? port=? [sslKeyStoreFile=?] [sslKeyStorePassword=?] title=? mongoDbConnection=? mongoDbName=? provider=? consumerAccessToken=? testConsumerMatch=? [persistent=?] 
```

Вместо `?` подставьте значения:
- _distributionUrl_
  - URL исходного сервера дистрибуции
- _accessToken_
  - Access Token сервиса _builder_ исходного сервера дистрибуции
    - Зайдите в _Settings/Accounts/Services_ и нажмите на значок ключа для _builder_.  
- _cloudProvider_
  - тип облачного сервиса, если сервер дистрибуции находится в облаке:
    - Azure - единственное на данный момент поддерживаемое облако
- _distribution_
  - уникальное имя сервера дистрибуции
- _directory_
  - каталог для установки
- _host_
  - DNS имя для сервиса
- _port_
  - порт для сервиса
- _[sslKeyStoreFile]_
  - файл с приватным ключом и сертификатом для сервиса https
- _[sslKeyStorePassword]_
  - пароль к файлу с приватным ключом и сертификатом для сервиса https
- _title_
  - короткое описание сервера дистрибуции
- _mongoDbConnection_
  - URL подсоединения к MongoDB, например mongodb://localhost:27017
- _mongoDbName_
  - имя базы данных MongoDB
- _provider_
  - имя исходного сервера дистрибуции
- _consumerAccessToken_
  - Access Token для учётной записи _consumer_
    - Зайдите в _Settings/Accounts/Distribution Consumers_ и нажмите на значок ключа для соответствующей записи.
- _testConsumerMatch_
  - имя сервера дистрибуции тестовой системы
- _persistent_
  - в настоящее время только для установки на Linux.
  - _true_, если сервер дистрибуции устанавливается как сервис systemd Linux.

Выполните скрипт _clone_distribution.sh_. 
После завершения скрипта должен запуститься процесс сервера дистрибуции.

### Настройка сервера дистрибуции

#### Конфигурационный файл 

Файл конфигурации _distribution.json_ находится в рабочем каталоге сервера дистрибуции и имеет следующий формат:

- _distribution_ : уникальное имя сервера дистрибуции
- _title_ : заголовок сервера дистрибуции
- _instance_ : идентификатор инстанции виртуальной мащины 
- _jwtSecret_ : ключ кодирования JWT
- _mongoDb_ - настройка подключения к Mongo DB
  - _connection_ : URL подключения 
  - _name_ : имя базы
- _network_
  - _port:number_ : номер серверного порта http или https
  - _[ssl]_ - настройки для сервиса https
    - _keyStoreFile_ : файл jks с ключами и сертификатами
    - _keyStorePassword_ : пароль доступа к key store
- _versions_ - история версий
  - _maxHistorySize:number_ : максимальное количество версий сервиса в истории
- _serviceStates_ - состояние сервисов 
  - _expirationTimeout:FiniteDuration_ : длительность хранения в базе последнего состояния сервиса. 
     Если в течение этого времени состояние не обновляется, инстанция сервиса считается умершей.
- _logs_ - журналы сервисов
  - _expirationTimeout:FiniteDuration_ : длительность хранения в базе записи в журнал
- _faultReports_ - отчёты о сбоях
  - _expirationTimeout:FiniteDuration_ : длительность хранения в базе записи отчёта о сбое
  - _maxReportsCount:number_ : максимальное количество отчётов о сбоях в базе

Структура _FiniteDuration_:
- unit - единицы времени: "SECONDS", "HOURS", "MINUTES", "DAYS"
- length:number - количество единиц
  
### Работа сервера дистрибуции

#### Запросы GraphQL

Сервер дистрибуции принимает запросы GraphQL от пользователей, от других сервисов v-update и других серверов дистрибуции.
Запросы GraphQL управляют:
  - учётными записями
  - настройками связи между серверами дистрибуции
  - репозиториями исходных кодов
  - профилями сервисов
  - версиями разработчика
  - желательными версиями разработчика
  - версиями клиента
  - желательными версиями клиента
  - сохранением/поиском состояния сервисов
  - сохранением/поиском журналов сервисов
  - сохранением/поиском отчётов о сбоях

Для создания версий сервер дистрибуции запускает _builder_ локально или на другом сервере дистрибуции.

#### Запись журналов

Журналы сервисов, включая сам _distribution_, записываются в коллекцию MongoDB.
Каждая запись коллекции содержит одну запись журнала и состоит из следующих полей:
- _service_ - название сервиса
- _instance_ - идентификатор инстанции сервера, на котором выполняется сервис 
- _directory_ - текущий каталог процесса
- _process_ - идентификатор процесса
- _[task]_ - если запись получена в процессе выполнения задачи, идентификатор задачи
- payload 
  - _time:Date_ - время и дата создания записи
  - _level_ - уровень отладки
  - _unit_ - функциональный блок
  - _message_ - сообщение журнала
  - _terminationStatus:Boolean_ - статус завершения 
    - если определена _task_ - задачи
    - иначе - процесса  

Схема GraphQL имеет запрос для поиска записей журналов по разным критериям.
Можно также выполнить GraphQL _subscription_ на журнал записей, и тогда новые записи в журнал будут
поступать клиенту GraphQL в реальном времени.

#### Задачи

Действия, асинхронные к запросам GraphQL, оформляются в задачи.
Информация о задачах записывается в коллекцию MongoDB в следующем формате:
- _id_ - идентификатор задачи
- _type_ - тип задачи
- _parameters_ - набор параметров, в формате
  - _name_ - имя параметра
  - _value_ - значение параметра
- _creationTime:Date_ - время и дата создания задачи

Журнал выполнения и статус завершения задачи можно получить из журналов.

<a name="service_state"></a>
#### Обработка состояния сервисов

_Updater_ периодически присылает _distribution_ состояние выполняющихся сервисов. 
Состояние сервисов записываются в коллекцию MongoDB, и на локальный диск.

Формат записи:
- _distribution_ - идентификатор сервера дистрибуции
- _payload:ServiceState_
  - _instance_ - идентификатор инстанции
  - _service_ - название сервиса
  - _directory_ - рабочий каталог сервиса
  - _state:ServiceState_ - состояние сервиса
    - _time_ - время получения состояния
    - _[installTime]_ - время, когда сервис был установлен
    - _[startTime]_ - время запуска сервиса
    - _[version]_ - текущая версия сервиса
    - _[updateToVersion]_ - если сервис в данный момент обновляется, целевая версия
    - _[updateError]_ - ошибка обновления
    - _[failuresCount]_ - количество сбоев сервиса после установки и запуска _updater_
    - _[lastExitCode]_ - последний код завершения

#### Обработка отчётов о сбоях

Отчёты о сбоях сервисов записываются в коллекцию MongoDB, и на локальный диск.

Формат записи:
- _distribution_ - идентификатор сервера дистрибуции 
- _payload_
  - _id_ - идентификатор отчёта о сбое
  - _info_ - последнее состояние сервиса
    - _time_ - время получения состояния
    - _instance_ - идентификатор инстанции сервера
    - _service_ - название сервиса
    - _[serviceRole]_ - роль сервиса
    - _serviceDirectory_ - рабочий каталог сервиса 
    - _serviceState:ServiceState_ - последнее состояние сервиса
    - _logTail:LogLine[]_ - 500 последних записей в журнале
  - _files[]_ - файлы с информацией о сбое

#### Взаимодействие с другими серверами дистрибуции

Сервера дистрибуции могут объединяться в сеть по принципу поставщик/потребитель.

Поставщик предоставляет для потребителя заданные в его _profile_ сервисы.
Если у поставщика появились новые версии, потребитель может скачать их, и установить у себя.
Таким образом компания, разрабатывающая сервисы, может завести учётную запись потребителя
на своём сервере дистрибуции и определить список сервисов, которые она хочет ему отдавать.

Потребитель периодически отправляет поставщику информацию об установленных сервисах поставщика:
- Номера установленных версий
- Состояние инстанций
- Отчёты о сбоях

Поставщик также может выполнять _builder_ для потребителя.
Это необходимо, если на сервере потребителя нет достаточных ресурсов для сборки версии, или поставщик
обладает необходимым набором системных библиотек.

Возможно автоматическое обновление сервисов потребителя при появлении новых версий разработчика у поставщика.
Потребитель периодически проверяет наличие обновлений, и если обновления обнаружены, запускает задачу обновления.

### Обновление сервера дистрибуции

Сервер дистрибуции является сервисом v-update, а это означает, что он собирается и обновляется по
общим правилам. Сервер периодически сверяет свою текущую версию с клиентской версией в базе данных,
и если они различаются, перезапускается. Скрипты после завершения сервера дистрибуции устанавливают и запускают 
новую версию.

Завершение сервера дистрибуции также происходит, если обновилась клиентская версия скриптов. В этом случае
скрипты обновляют сами себя и запускают сервер дистрибуции снова.

### История документов в Mongo DB

Все документы v-update хранятся в коллекциях специального формата.

У каждого документа, кроме содержательной части, есть дополнительные поля:
- __sequence_ - порядковый номер добавления/модификации документа в коллекции
  - индексировано во возрастанию и убыванию
- __modifyTime_ - время добавления/модификации документа
- __archiveTime_ - время удаления документа
  - индексировано во возрастанию с опцией expire after 7 days

Добавление специальных полей позволяет:
- Сортировать документы в коллекции по порядку их добавления/изменения
- Выяснить дату последней модификации документа
- Хранить историю удалённых документов в течение 7 дней, и при необходимости восстанавливать старые версии

## Builder

Производит:
- Сборку/установку сервера дистрибуции
- Сборку версий разработчика
- Сборку клиентских версий

### Установка и обновление

Builder устанавливается сервером дистрибуции в каталог _<distributionDir>_/builder/_<distribution>_.
Здесь:
- _distributionDir_ - каталог сервера дистрибуции
- _distribution_ - имя сервера дистрибуции, для которого производится сборка

Установка и обновление производится скриптами v-update с сервера дистрибуции, для которого производится сборка.

### Установка сервера дистрибуции

Была описана в разделе (см [Создание сервера дистрибуции](#create_distribution_server)).

<a name="build_developer_version"></a>
### Сборка версии разработчика

Сервер дистрибуции выполняет команду:

`builder.sh buildDeveloperVersion service=? version=? sourceRepositories=? [macroValues=?] [buildClientVersion=true/false] comment=?`

Здесь:
- _service_ - сервис, для которого изготавливается версия
- _version_ - номер новой версии
- _sourceRepositories_ - конфигурация исходных репозиториев, Json формата:
  - []
    - _name_ - имя репозитория, является именем каталога, куда копируются исходники
    - _git_
      - _url_ - URL доступа к Git репозиторию
      - _branch_ - branch Git репозитория
      - _[cloneSubmodules]_ - клонировать вместе с submodules
    - _[subDirectory]_ - имя каталога внутри репозитория. 
                         Если указан, исходники берутся только из этого каталога.
- _privateFiles[]_ - список приватных файлов для включения в версию, массив в формате Json
- _[macroValues]_ - значения макро в настройках в Json формате:
  - []
    - _name_ - имя
    - _value_ - значение
- _comment_ - комментарий к новой версии

Этапы сборки версии:
- В каталог `<builderDir>/developer/services/<service>/source` производится _clone_ или _pull_ репозиториев,
указанных в _sourceRepositories_. Каждый репозиторий копируется в подкаталог с названием _name_ из конфигурации.
- Сборка версии происходит в первом каталоге, описанном в _sourceRepositories_. В этом каталоге должен находиться
описанный выше файл _update.json_. Ссылка на другой каталог из другого репозитория должна выглядеть, как `../<name>`.
- При сборке выполняются команды из _build/buildCommands_. Если в описании команд присутствуют макро, будут произведены макроподстановки:
  - макро _%%version%%_ заменяется на собираемую версию
  - прочие значения берутся из параметра _values_
- Если задан _exitCode_, его значение сравнивается с кодом возврата команды.
- Если задано _outputMatch_, выход команды должен соответствовать этому регулярному выражению.  
- После выполнения команд сборки выполняется копирование файлов из каталога 
`<builderDir>/developer/services/<service>/source` в каталог `<builderDir>/developer/services/<service>/build`
файлов, указанных в _copyFiles_.
- Секция _install_ файла update.json записывается в файл install.json каталога сборки.
- Файлы _privateFiles_ скачиваются с сервера дистрибуции и записываются в каталог сборки.
- Упаковка в ZIP-файл каталога сборки и закачка образа версии и информации о версии
на сервер дистрибуции.
- После на репозиториях исходных файлов устанавливаются метки с номером сгенерированной версии.
Формат метки: `<service>-<distribution>-<build>`.

<a name="build_client_version"></a>
### Сборка клиентской версии

Сервер дистрибуции выполняет команду:

`builder.sh buildClientVersion service=? developerVersion=? clientVersion=? [settingsRepositories=?] [macroValues=?]`

Здесь:
- _service_ - сервис, для которого изготавливается версия
- _developerVersion_ - версия разработчика, с которой изготавливается клиентская версия
- _clientVersion_ - номер клиентской версии
- _[settingsRepositories]_ - репозитории с настройками в Json формате:
  - []
    - _name_ - имя репозитория, является именем каталога, куда копируются настройки
    - _git_
      - _url_ - URL доступа к Git репозиторию
      - _branch_ - branch Git репозитория
      - _[cloneSubmodules]_ - клонировать вместе с submodules
    - _[subDirectory]_ - имя каталога внутри репозитория.
      Если указан, настройки берутся только из этого каталога
- _privateFiles[]_ - список приватных файлов для включения в версию, массив в формате Json
- _[macroValues]_ - значения макро в настройках в Json формате как в _buildDeveloperVersion_

Этапы сборки версии:
- С сервера дистрибуции скачивается образ версии и распаковывается 
  в каталог `<builderDir>/client/services/<service>/build`.
- В каталог `<builderDir>/client/services/<service>/settings` производится _clone_ репозиториев,
  указанных в _settingsRepositories_. Каждый репозиторий копируется в подкаталог с названием _name_ из конфигурации.
- Дополнительные настройки сливаются с настройками версии разработчика 
  (см [Кастомизация настроек для клиента](#settings_customization))
- Файлы _privateFiles_ скачиваются с сервера дистрибуции и записываются в каталог сборки.
- Каталог сборки упаковывается в ZIP-файл каталога сборки, который закачивается на сервер дистрибуции 
  вместе с информацией о версии.

## Updater

Устанавливает, запускает и обновляет инстанции сервисов. 

### Установка updater

Производится скриптом _.updater_setup.sh_. Скрипт создаёт файл конфигурации _updater.json_ и устанавливает
_updater_ как сервис _systemd_.

`.updater_setup.sh <cloudProvider> <name> <services> <distributionUrl> <accessToken> [environment]`

Здесь:
- _cloudProvider_ 
  - Имя облачного сервиса. Единственное поддерживаемое значение на данный момент - _Azure_.
- _name_
  - Часть имени сервиса _systemd_. Полное имя будет таким `update-${name}.service`. 
- _services_
  - Сервисы для запуска, через запятую, к имени сервиса может быть добавлена
    роль, с которой он запускается `-<role>`
- _distributionUrl_
  - URL сервера дистрибуции
- _accessToken_
  - Access token для доступа к серверу дистрибуции

### Файл конфигурации updater.json

- instanceId
  - Идентификатор инстанции виртуальной машины
- distributionUrl
  - URL сервера дистрибуции
- accessToken
  - Access token для доступа к серверу дистрибуции

### Запуск updater

`updater.sh runServices services=<service1>[-<role>][,...]`

Здесь:
- services - сервисы для запуска, как для _.updater_setup.sh_ 

### Работа updater

Для каждого из сервисов, указанных в аргументах, _updater_ выполняет следующее.

#### Установка сервиса

С сервера дистрибуции скачивается желательная клиентская версия для данного сервиса и распаковывается 
в каталог сборки _<service[-role]>/new_.

Из каталога сборки зачитывается файл _install.json_.
В каталоге сборки последовательно выполняются команды _installCommands_ с предварительно выполненными 
расширениями макросов:
- _role_ - роль сервиса
- _version_ - версия разработчика

Каталог сборки _<service[-role]>/new_ переименовывается в каталог рабочий _<service[-role]>/current_.

В рабочем каталоге последовательно выполняются команды _postInstallCommands_ 
с предварительно выполненными расширениями макросов:
- _role_ - роль сервиса
- _version_ - версия разработчика

Команды установки разделены на две группы, чтобы уменьшить время неактивности сервиса при обновлении.
Группа _installCommands_ содержит команды, которые могут выполняться из любого каталога.
Они выполняются до остановки старой версии.
В группе _postInstallCommands_ содержатся команды, которые могут выполняться только из рабочего каталога.

#### Запуск сервиса

В каталоге _<service[-role]>/current_ выполняется команда _runService_ 
с предварительно выполненными расширениями макросов:
- _role_ - роль сервиса
- _version_ - версия разработчика

#### Запись журнала работы

Сервис должен писать журнал работы на стандартный вывод. Формат строки журнала должен быть следующим:
`date level unit message`

Здесь:
_date_ - дата в формате `yyyy-MM-dd HH:mm:ss.SSS`
_level_ - уровень отладки: _TRACE_, _DEBUG_, _INFO_, _WARN_ или _ERROR_
_unit_ - функциональный блок
_message_ - сообщение журнала

Стандартный вывод и вывод ошибок сервиса перехватываются _updater_.

Если в конфигурации определена секция _writeLogs_, полученные строки журнала работы записываются 
в каталог _<service[-role]>/current/<directory>_ по указанным правилам.

Если параметр _uploadLogs_ задан как `true`, строки журнала также отправляются на сервер дистрибуции
в реальном времени.

#### Отправка состояния сервисов

Раз в 10 сек _updater_ отправляет состояние сервисов _ServiceState_ на сервер дистрибуции
(см [Обработка состояния серисов](#service_state)). 

#### Обработка сбоя сервисов

Если сервис неожиданно завершается, формируется отчёт о сбое. 

Регулярное выражение, содержащееся в настройке _faultFilesMatch_, описывает файлы с информацией о сбое,
которые могут образоваться при сбое сервиса. Например, это может быть файл _core dump_, или
файл Java hprof.

Отчёт о сбое записывается в каталог <service[-role]>/faults/<time>.
Кроме того, отчёт о сбое архивируется и отправляется на сервер дистрибуции.

Если значение настройки _restartOnFault_ не определено, или определено как _true_, сервис запускается 
заново.

#### Аварийное прерывание работы сервиса _updater_-ом

Настройка _restartConditions_ описывает условия для принудительного завершения сервиса.

Для ограничения сервиса по количеству использованной памяти указывается параметр _maxMemoryMB_.
При превышении процессом сервиса указанного лимита, сервис принудительно завершается.

Для ограничения сервиса по потреблению CPU указывается параметр _maxCpu_.
Если процесс сервиса потребляет более указанных _percents_ в течение _durationSec_, 
сервис принудительно завершается.

Параметр _makeCore_ указывает на необходимость завершения сервиса сигналом SIGQUIT, 
для образования _core dump_.

Интервал проверки наступления условий прерывания работы сервиса задаётся параметром _checkTimeoutMs_.

#### Обновление версии

Если на сервере дистрибуции меняется желательная версия сервиса:
- Скачивается новая версия
- Выполняются команды из _installCommands_
- Останавливается старая версия
  - Процессу сервиса и его потомкам отправляется сигнал завершения
- Журналы работы сервисов копируются в каталог <service[-role]>/log.history/<version>-<time>.log>
- Выполняются команды из _postInstallCommands_
- Запускается новая версия

### Самообновление

Если на сервере дистрибуции меняется желательная версия _updater_ или _scripts_, _updater_ скачивает
новые версии, но не обновляется сразу, а ждёт, пока не обновится один из сервисов. Это сделано для того,
чтобы не прерывать работу сервисов без необходимости. 

Как только на сервере дистрибуции меняется желательная версия одного из сервисов, _updater_ скачивает обновления
и перезапускается.

# Distribution Dashboard

## Settings

Откройте ссылку на сервер дистрибуции в бравсере.
Введите Account Name: admin и Password: admin.
Вы зашли в систему как администратор.

Смените начальный пароль. Для этого войдите в _Settings/Accounts/Users_, выберите пользователя _admin_,
и нажмите "Change Password".

### Accounts

Администрируются в _Settings/Accounts_.

#### Users

Заведите пользователей Dashboard, разработчиков и администраторов.
В информации о версии сервиса будет отображаться имя создавшего его пользователя.

<a name="services_accounts"></a>
#### Services

Учётные записи сервисов v-update. После установки сервера дистрибуции уже присутствуют
записи _builder_ и _updater_.
Нажав на иконку с ключом, можно получить Access Key для данного сервиса.
Access Key используется в скриптах запуска и обновления сервисов.

#### Distribution Consumers

Учётные записи других серверов дистрибуции.
В учётной записи, помимо прочего, указываются:
- URL сервера дистрибуции. Его использует _builder_ для удалённой сборки сервиса.
- Services Profile - список разрабатываемых сервисов, которые поставляются данному серверу дистрибуции.

### Build

Настройки сборки версий.

#### Developer

Настройки для сборки версии разработки.
Включает в себя:
- Задание сервера дистрибуции для запуска _builder_.
- Описание параметров для запуска _builder_:
  - Сервер дистрибуции для запуска builder. 
  - Окружение.
  - Репозитории исходных кодов, значения макросов.
    (см [Сборка версии разработчика](#build_developer_version))
  - Приватные файлы.
    - Path - путь к файлу в каталоге сборки.
    - File To Upload - локальный файл для загрузки.

#### Client

Настройки для сборки клиентской версии.
Включает в себя:
- Задание сервера дистрибуции для запуска _builder_.
- Описание параметров для запуска _builder_:
  - Сервер дистрибуции для запуска builder. 
  - Окружение.
  - Репозитории настроек, значения макросов.
    (см [Сборка клиентской версии](#build_client_version))
  - Приватные файлы.
    - Path - путь к файлу в каталоге сборки.
    - File To Upload - локальный файл для загрузки.

### Profiles

Профили сервисов - это подмножества списка сервисов для разработки.
Далее профиль назначается _distribution consumer_ в настройках.

Для обозначения разрабатываемых сервисов, для которых также создаются клиентские версии
на данном сервере дистрибуции, определяется профиль _self_.

### Providers

Здесь определяются поставщики данного сервера дистрибуции.
Среди прочего, для поставщика определяются:
- _Access Token_ - Ключ доступа при запросах к серверу
- _Test Consumer_ - Имя потребителя, после тестирования которым, сервисы могут быть установлены на данном сервере
- _Upload State Interval_ - Интервал загрузки состояния на поставщика
- _Auto Update_ - При изменении желательных версий на поставщике они автоматически
                  загружаются и устанавливаются на сервер дистрибуции. Интервал для проверки - 10 сек.

## Сборка версии разработчика

После того, как будут определены сервисы для разработки, откройте _Build/Developer_.
Покажется таблица сервисов и их версий:
- Service - разрабатываемый сервис
- Last Version, Author, Time, Comment - последняя версия, автор и время генерации сервиса
- Status
  - In Process - версия собирается данный момент
  - Completed - версия собрана

Если в текущий момент не производится сборки версии сервиса, можно запустить задачу
создания новой версии, выбрав запись.
Номер новой версии будет на единицу больше старой, либо его можно назначить вручную.
Если данный сервис присутствует в профиле клиентских сервисов 'own', по-умолчанию
будет предложено создать также клиентскую версию.

При нажатии на _"Get Lat Commit Comment"_ будет произведено чтение истории изменений репозиториев сервиса.
В поле _Comment_ отобразится комментарий к последнему коммиту в репозитории сервиса.

После запуска задачи создания новой версии отобразится журнал выполнения задачи
в реальном времени. Можно прервать задачу создания версии.

Если версия создаётся в текущий момент, выбрав запись, можно посмотреть журнал создания в 
реальном времени. Можно прервать задачу создания версии.

## Сборка клиентских версий

Откройте _Build/Client_, если определены сервисы для разработки, иначе _Build_.

Если клиентские версии не создаются в текущий момент, появится диалог запуска сборки.

### С закачкой от поставщика

Если выбран поставщик, покажется таблица сервисов этого поставщика, применительно к профилю потребителя:
- Service - сервис для установки на клиента
- Provider Desired Version - желаемая версия сервиса у поставщика
- Developer Desired Version - желаемая версия разработчика на сервере дистрибуции
- Client Desired Version - желаемая клиентская версия
- Tested Developer Version - последняя протестированная версия

Уже выбраны для сборки сервисы, для которых нет клиентских версий с желаемой версией от поставщика.
Остальные сервисы выбрать для сборки нельзя.

### Без закачки от поставщика

Если не выбран поставщик, отображаются сервисы собственной разработки, описанные в профиле 'own':
- Service - сервис для установки на клиента
- Developer Desired Version - желаемая версия разработчика на сервере дистрибуции
- Client Desired Version - желаемая клиентская версия

Уже выбраны для сборки сервисы, для которых нет клиентских версий 
с желаемой версией разработчика на сервере дистрибуции.

Можно пометить для сборки и сервисы, для которых уже собраны клиентские версии для данной версии
разработчика. Это имеет смысл, если конфигурация клиента поменялась и нужно пересобрать клиентскую версию.
В этом случае будет сгенерирована клиентская версия с новым номером генерации клиентской версии.

### Мониторинг процесса сборки

После запуска сборки отобразится журнал выполнения задачи в реальном времени. 
При необходимости, можно прервать задачу сборки.

Журнал выполнения задачи также отображается, если при открытии диалога сборки клиентских версий 
уже происходит сборка.  

### Пометка версий как протестированных

Перед установкой сервисов на рабочие сервера, желательно провести комплексное тестирование.
Для того чтобы сервисы могли ставиться на рабочие сервера только после того, как они будут
протестированы, v-update имеет механизм пометки списка версий сервисов, как протестированных.

Например, есть некий сервер дистрибуции для рабочих серверов с именем _production_, 
в конфигурации которого задан тестовый сервер дистрибуции _test_. 
Установка новых версий сервисов на сервер _production_ произойдёт только в случае, 
если они были протестированы на _test_, и помечены _test_-ом, как протестированные.

## Желаемые версии

В этом разделе можно редактировать списки желаемых версий разработчика и клиента.
Например, можно откатить неудачно установленное обновление, или несколько последних обновлений.
Это возможно, поскольку сервер дистрибуции хранит несколько последних установленных версий и историю
изменений желаемых версий.

Редактирование производится одним из двух способов.

### Откат до предыдущей редакции списка

Чтобы перейти к более старой редакции, нажмите кнопку 'вниз', для перехода к более новой редакции нажмите 'вверх'.
Время создания списка и автор указываются вверху. Записи в таблице отображают желаемые версии выбранной редакции
в сравнении с последней редакцией:
- стандартным шрифтом отображаются неизменившиеся записи
- жирным шрифтом - у сервиса изменилась желаемая версия
- красным цветом - в этой редакции нет сервиса, который есть в последней редакции
- зелёным цветом - в этой редакции есть сервис, которого нет в последней редакции

Для назначения списка желаемых версий текущим, нажмите _Save_. 

### Назначение версий вручную

Также можно произвольно назначать версии сервисам. Для этого нужно кликнуть по версии сервиса, 
после чего появится список доступных версий в истории.
При изменении версии, запись с сервисом выделяется жирным шрифтом. 

Для назначения отредактированного списка желаемых версий текущим, нажмите _Save_.

## Наблюдение за журналами

Можно просматривать журналы в контексте выполнения задачи или сервиса.

### Наблюдение за журналами задач

Отображается таблица задач в обратном хронологическом порядке:
- Creation Time - время запуска задачи
- ID - идентификатор задачи
- Type - тип задачи 
- Parameters - параметры задачи в формате: _name:value_
- Active - задача выполняется в данное время

При выборе задачи отображаются записи в журнал задачи:
- Time - время записи в журнал
- Level - уровень отладки
- Line - запись в журнал

Возможна выборка по:
- Level - уровню отладки журналов
  - при выбранном уровне отображаются записи этого уровня и выше
- Find Text - образцу текста

При выборе _Follow_ отображается конец журнала, новые записи в журнал отображаются в реальном времени.

### Наблюдение за журналами сервиса

Выполняется поиск журналов выбранного сервиса с опциональной выборкой по:
- Instance - идентификатору инстанции
- Directory - рабочему каталогу сервиса
- Process - номеру процесса сервиса
- Level - уровню отладки журналов
  - при выбранном уровне отображаются записи этого уровня и выше
- From/To - диапазону времени
- Find Text - образцу текста

При выборе _Follow_ отображается конец журнала, новые записи в журнал отображаются в реальном времени.

## Наблюдение за отчётами о сбоях

На сервер дистрибуции поступают отчёты о сбоях его сервисов, и сервисов его потребителей.
По умолчанию отображаются все отчёты в убывающем хронологическом порядке.
Можно сузить диапазон поиска по критериям:
- Distribution - сервер дистрибуции
- Service - имя сервиса
- From/To - диапазон времени

При выборе отчёта отображается информация о сбое и последние записи в журнал работы сервиса.

Возможно скачивание ZIP архива журнала работы в файл. 

## Dashboard

Страница _Dashboard_ предназначена для оперативного контроля работы _v-update_.
С её помощью можно увидеть, как живёт _v-update_, быстро найти возможные проблемы, 
откатить установленные версии. 

Для отображения информации под разными углами, служат панели. 

### Панель Versions

Предназначена для мониторинга дистрибуции и работы версий сервисов.

Показывается таблица с полями:
- Service - имя сервиса
- Developer Desired Version - версия разработчика
- Client Desired Version - клиентская версия
- Working Version - клиентская версия, выполняющаяся в данный момент
- Directory - рабочий каталог сервиса
- Instances - список идентификаторов инстанций
- Info - состояние сервиса

Таким образом, запись этой таблицы отображает полный путь дистрибуции версии сервиса от 
версии разработчика до версии, работающей на инстанции и её состояния.

Если путь дистрибуции пройден полностью, все три версии будут одинаковыми.
Иначе, отображается красным цветом первая несовпадающая в пути версия.
При выборе _Only Alerts_ отображаются только сервисы с неполным путём дистрибуции.

При выборе потребителя, отображаются версии, установленные на этого потребителя. 

### Панель Last Developer Versions

Отображаются последние выпущенные и выпускаемые версии разработчика.

### Панель Last Client Versions

Отображаются последние выпущенные и выпускаемые версии клиента.


