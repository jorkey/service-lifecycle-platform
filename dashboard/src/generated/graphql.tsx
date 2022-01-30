import { gql } from '@apollo/client';
import * as Apollo from '@apollo/client';
export type Maybe<T> = T | null;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
const defaultOptions =  {}
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  /** The `BigInt` scalar type represents non-fractional signed whole numeric values. BigInt can represent arbitrary big values. */
  BigInt: BigInt;
  Date: Date;
  /** The `Long` scalar type represents non-fractional signed whole numeric values. Long can represent values between -(2^63) and 2^63 - 1. */
  Long: number;
};

export enum AccountRole {
  Updater = 'Updater',
  Builder = 'Builder',
  DistributionConsumer = 'DistributionConsumer',
  Administrator = 'Administrator',
  Developer = 'Developer',
  None = 'None'
}


export type BuildInfo = {
  __typename?: 'BuildInfo';
  author: Scalars['String'];
  sources: Array<Repository>;
  time: Scalars['Date'];
  comment: Scalars['String'];
};

export type BuildInfoInput = {
  author: Scalars['String'];
  sources: Array<RepositoryInput>;
  time: Scalars['Date'];
  comment: Scalars['String'];
};

export type BuildServiceConfig = {
  __typename?: 'BuildServiceConfig';
  service: Scalars['String'];
  distribution?: Maybe<Scalars['String']>;
  environment: Array<NamedStringValue>;
  repositories: Array<Repository>;
  privateFiles: Array<FileInfo>;
  macroValues: Array<NamedStringValue>;
};

export type ClientDesiredVersion = {
  __typename?: 'ClientDesiredVersion';
  service: Scalars['String'];
  version: ClientDistributionVersion;
};

export type ClientDesiredVersionDeltaInput = {
  service: Scalars['String'];
  version?: Maybe<ClientDistributionVersionInput>;
};

export type ClientDesiredVersionInput = {
  service: Scalars['String'];
  version: ClientDistributionVersionInput;
};

export type ClientDistributionVersion = {
  __typename?: 'ClientDistributionVersion';
  distribution: Scalars['String'];
  developerBuild: Array<Scalars['Int']>;
  clientBuild: Scalars['Int'];
};

export type ClientDistributionVersionInput = {
  distribution: Scalars['String'];
  developerBuild: Array<Scalars['Int']>;
  clientBuild: Scalars['Int'];
};

export type ClientVersionInfo = {
  __typename?: 'ClientVersionInfo';
  service: Scalars['String'];
  version: ClientDistributionVersion;
  buildInfo: BuildInfo;
  installInfo: InstallInfo;
};

export type ClientVersionInfoInput = {
  service: Scalars['String'];
  version: ClientDistributionVersionInput;
  buildInfo: BuildInfoInput;
  installInfo: InstallInfoInput;
};

export type ClientVersionInput = {
  developerBuild: Array<Scalars['Int']>;
  clientBuild: Scalars['Int'];
};

export type ConsumerAccountInfo = {
  __typename?: 'ConsumerAccountInfo';
  account: Scalars['String'];
  name: Scalars['String'];
  role: AccountRole;
  properties: ConsumerAccountProperties;
};

export type ConsumerAccountProperties = {
  __typename?: 'ConsumerAccountProperties';
  profile: Scalars['String'];
  url: Scalars['String'];
};

export type ConsumerAccountPropertiesInput = {
  profile: Scalars['String'];
  url: Scalars['String'];
};


export type DeveloperDesiredVersion = {
  __typename?: 'DeveloperDesiredVersion';
  service: Scalars['String'];
  version: DeveloperDistributionVersion;
};

export type DeveloperDesiredVersionDeltaInput = {
  service: Scalars['String'];
  version?: Maybe<DeveloperDistributionVersionInput>;
};

export type DeveloperDesiredVersionInput = {
  service: Scalars['String'];
  version: DeveloperDistributionVersionInput;
};

export type DeveloperDistributionVersion = {
  __typename?: 'DeveloperDistributionVersion';
  distribution: Scalars['String'];
  build: Array<Scalars['Int']>;
};

export type DeveloperDistributionVersionInput = {
  distribution: Scalars['String'];
  build: Array<Scalars['Int']>;
};

export type DeveloperVersionInfo = {
  __typename?: 'DeveloperVersionInfo';
  service: Scalars['String'];
  version: DeveloperDistributionVersion;
  buildInfo: BuildInfo;
};

export type DeveloperVersionInfoInput = {
  service: Scalars['String'];
  version: DeveloperDistributionVersionInput;
  buildInfo: BuildInfoInput;
};

export type DeveloperVersionInput = {
  build: Array<Scalars['Int']>;
};

export type DistributionFaultReport = {
  __typename?: 'DistributionFaultReport';
  distribution: Scalars['String'];
  payload: ServiceFaultReport;
};

export type DistributionInfo = {
  __typename?: 'DistributionInfo';
  distribution: Scalars['String'];
  title: Scalars['String'];
};

export type DistributionProviderInfo = {
  __typename?: 'DistributionProviderInfo';
  distribution: Scalars['String'];
  url: Scalars['String'];
  accessToken: Scalars['String'];
  testConsumer?: Maybe<Scalars['String']>;
  uploadState?: Maybe<Scalars['Boolean']>;
  autoUpdate?: Maybe<Scalars['Boolean']>;
};

export type DistributionServiceState = {
  __typename?: 'DistributionServiceState';
  distribution: Scalars['String'];
  payload: InstanceServiceState;
};

export type FaultInfo = {
  __typename?: 'FaultInfo';
  time: Scalars['Date'];
  instance: Scalars['String'];
  service: Scalars['String'];
  serviceRole?: Maybe<Scalars['String']>;
  serviceDirectory: Scalars['String'];
  state: ServiceState;
  logTail: Array<LogLine>;
};

export type FaultInfoInput = {
  time: Scalars['Date'];
  instance: Scalars['String'];
  service: Scalars['String'];
  serviceRole?: Maybe<Scalars['String']>;
  serviceDirectory: Scalars['String'];
  state: ServiceStateInput;
  logTail: Array<LogLineInput>;
};

export type FileInfo = {
  __typename?: 'FileInfo';
  path: Scalars['String'];
  time: Scalars['Date'];
  length: Scalars['Long'];
};

export type FileInfoInput = {
  path: Scalars['String'];
  time: Scalars['Date'];
  length: Scalars['Long'];
};

export type GitConfig = {
  __typename?: 'GitConfig';
  url: Scalars['String'];
  branch: Scalars['String'];
  cloneSubmodules?: Maybe<Scalars['Boolean']>;
};

export type GitConfigInput = {
  url: Scalars['String'];
  branch: Scalars['String'];
  cloneSubmodules?: Maybe<Scalars['Boolean']>;
};

export type InstallInfo = {
  __typename?: 'InstallInfo';
  account: Scalars['String'];
  time: Scalars['Date'];
};

export type InstallInfoInput = {
  account: Scalars['String'];
  time: Scalars['Date'];
};

export type InstanceServiceState = {
  __typename?: 'InstanceServiceState';
  instance: Scalars['String'];
  service: Scalars['String'];
  directory: Scalars['String'];
  state: ServiceState;
};

export type InstanceServiceStateInput = {
  instance: Scalars['String'];
  service: Scalars['String'];
  directory: Scalars['String'];
  state: ServiceStateInput;
};

export type LogLine = {
  __typename?: 'LogLine';
  time: Scalars['Date'];
  level: Scalars['String'];
  unit: Scalars['String'];
  message: Scalars['String'];
  terminationStatus?: Maybe<Scalars['Boolean']>;
};

export type LogLineInput = {
  time: Scalars['Date'];
  level: Scalars['String'];
  unit: Scalars['String'];
  message: Scalars['String'];
  terminationStatus?: Maybe<Scalars['Boolean']>;
};


export type Mutation = {
  __typename?: 'Mutation';
  login: Scalars['String'];
  addUserAccount: Scalars['Boolean'];
  addServiceAccount: Scalars['Boolean'];
  addConsumerAccount: Scalars['Boolean'];
  changeUserAccount: Scalars['Boolean'];
  changeServiceAccount: Scalars['Boolean'];
  changeConsumerAccount: Scalars['Boolean'];
  removeAccount: Scalars['Boolean'];
  setBuildDeveloperServiceConfig: Scalars['Boolean'];
  removeBuildDeveloperServiceConfig: Scalars['Boolean'];
  setBuildClientServiceConfig: Scalars['Boolean'];
  removeBuildClientServiceConfig: Scalars['Boolean'];
  addServicesProfile: Scalars['Boolean'];
  changeServicesProfile: Scalars['Boolean'];
  removeServicesProfile: Scalars['Boolean'];
  buildDeveloperVersion: Scalars['String'];
  addDeveloperVersionInfo: Scalars['Boolean'];
  removeDeveloperVersion: Scalars['Boolean'];
  setDeveloperDesiredVersions: Scalars['Boolean'];
  buildClientVersions: Scalars['String'];
  addClientVersionInfo: Scalars['Boolean'];
  removeClientVersion: Scalars['Boolean'];
  setClientDesiredVersions: Scalars['Boolean'];
  addProvider: Scalars['Boolean'];
  changeProvider: Scalars['Boolean'];
  removeProvider: Scalars['Boolean'];
  setProviderTestedVersions: Scalars['Boolean'];
  setTestedVersions: Scalars['Boolean'];
  setInstalledDesiredVersions: Scalars['Boolean'];
  setServiceStates: Scalars['Boolean'];
  addLogs: Scalars['Boolean'];
  addFaultReportInfo: Scalars['Boolean'];
  runBuilder: Scalars['String'];
  cancelTask: Scalars['Boolean'];
};


export type MutationLoginArgs = {
  account: Scalars['String'];
  password: Scalars['String'];
};


export type MutationAddUserAccountArgs = {
  account: Scalars['String'];
  name: Scalars['String'];
  role: AccountRole;
  password: Scalars['String'];
  properties: UserAccountPropertiesInput;
};


export type MutationAddServiceAccountArgs = {
  account: Scalars['String'];
  name: Scalars['String'];
  role: AccountRole;
};


export type MutationAddConsumerAccountArgs = {
  account: Scalars['String'];
  name: Scalars['String'];
  properties: ConsumerAccountPropertiesInput;
};


export type MutationChangeUserAccountArgs = {
  account?: Maybe<Scalars['String']>;
  name?: Maybe<Scalars['String']>;
  role?: Maybe<AccountRole>;
  oldPassword?: Maybe<Scalars['String']>;
  password?: Maybe<Scalars['String']>;
  properties?: Maybe<UserAccountPropertiesInput>;
};


export type MutationChangeServiceAccountArgs = {
  account?: Maybe<Scalars['String']>;
  name?: Maybe<Scalars['String']>;
  role?: Maybe<AccountRole>;
};


export type MutationChangeConsumerAccountArgs = {
  account?: Maybe<Scalars['String']>;
  name?: Maybe<Scalars['String']>;
  properties?: Maybe<ConsumerAccountPropertiesInput>;
};


export type MutationRemoveAccountArgs = {
  account: Scalars['String'];
};


export type MutationSetBuildDeveloperServiceConfigArgs = {
  service: Scalars['String'];
  distribution?: Maybe<Scalars['String']>;
  environment: Array<NamedStringValueInput>;
  repositories: Array<RepositoryInput>;
  privateFiles: Array<FileInfoInput>;
  macroValues: Array<NamedStringValueInput>;
};


export type MutationRemoveBuildDeveloperServiceConfigArgs = {
  service: Scalars['String'];
};


export type MutationSetBuildClientServiceConfigArgs = {
  service: Scalars['String'];
  distribution?: Maybe<Scalars['String']>;
  environment: Array<NamedStringValueInput>;
  repositories: Array<RepositoryInput>;
  privateFiles: Array<FileInfoInput>;
  macroValues: Array<NamedStringValueInput>;
};


export type MutationRemoveBuildClientServiceConfigArgs = {
  service: Scalars['String'];
};


export type MutationAddServicesProfileArgs = {
  profile: Scalars['String'];
  services: Array<Scalars['String']>;
};


export type MutationChangeServicesProfileArgs = {
  profile: Scalars['String'];
  services: Array<Scalars['String']>;
};


export type MutationRemoveServicesProfileArgs = {
  profile: Scalars['String'];
};


export type MutationBuildDeveloperVersionArgs = {
  service: Scalars['String'];
  version: DeveloperVersionInput;
  comment: Scalars['String'];
  buildClientVersion?: Maybe<Scalars['Boolean']>;
};


export type MutationAddDeveloperVersionInfoArgs = {
  info: DeveloperVersionInfoInput;
};


export type MutationRemoveDeveloperVersionArgs = {
  service: Scalars['String'];
  version: DeveloperDistributionVersionInput;
};


export type MutationSetDeveloperDesiredVersionsArgs = {
  versions: Array<DeveloperDesiredVersionDeltaInput>;
};


export type MutationBuildClientVersionsArgs = {
  versions: Array<DeveloperDesiredVersionInput>;
};


export type MutationAddClientVersionInfoArgs = {
  info: ClientVersionInfoInput;
};


export type MutationRemoveClientVersionArgs = {
  service: Scalars['String'];
  version: ClientDistributionVersionInput;
};


export type MutationSetClientDesiredVersionsArgs = {
  versions: Array<ClientDesiredVersionDeltaInput>;
};


export type MutationAddProviderArgs = {
  distribution: Scalars['String'];
  url: Scalars['String'];
  accessToken: Scalars['String'];
  testConsumer?: Maybe<Scalars['String']>;
  uploadState?: Maybe<Scalars['Boolean']>;
  autoUpdate?: Maybe<Scalars['Boolean']>;
};


export type MutationChangeProviderArgs = {
  distribution: Scalars['String'];
  url: Scalars['String'];
  accessToken: Scalars['String'];
  testConsumer?: Maybe<Scalars['String']>;
  uploadState?: Maybe<Scalars['Boolean']>;
  autoUpdate?: Maybe<Scalars['Boolean']>;
};


export type MutationRemoveProviderArgs = {
  distribution: Scalars['String'];
};


export type MutationSetProviderTestedVersionsArgs = {
  distribution: Scalars['String'];
  versions: Array<DeveloperDesiredVersionInput>;
};


export type MutationSetTestedVersionsArgs = {
  versions: Array<DeveloperDesiredVersionInput>;
};


export type MutationSetInstalledDesiredVersionsArgs = {
  versions: Array<ClientDesiredVersionInput>;
};


export type MutationSetServiceStatesArgs = {
  states: Array<InstanceServiceStateInput>;
};


export type MutationAddLogsArgs = {
  service: Scalars['String'];
  instance: Scalars['String'];
  process: Scalars['String'];
  task?: Maybe<Scalars['String']>;
  directory: Scalars['String'];
  logs: Array<LogLineInput>;
};


export type MutationAddFaultReportInfoArgs = {
  fault: ServiceFaultReportInput;
};


export type MutationRunBuilderArgs = {
  accessToken: Scalars['String'];
  arguments: Array<Scalars['String']>;
  environment?: Maybe<Array<NamedStringValueInput>>;
};


export type MutationCancelTaskArgs = {
  task: Scalars['String'];
};

export type NamedStringValue = {
  __typename?: 'NamedStringValue';
  name: Scalars['String'];
  value: Scalars['String'];
};

export type NamedStringValueInput = {
  name: Scalars['String'];
  value: Scalars['String'];
};

export type Query = {
  __typename?: 'Query';
  ping: Scalars['String'];
  distributionInfo: DistributionInfo;
  whoAmI: UserAccountInfo;
  accountsList: Array<Scalars['String']>;
  userAccountsInfo: Array<UserAccountInfo>;
  serviceAccountsInfo: Array<ServiceAccountInfo>;
  consumerAccountsInfo: Array<ConsumerAccountInfo>;
  accessToken: Scalars['String'];
  buildDeveloperServicesConfig: Array<BuildServiceConfig>;
  buildClientServicesConfig: Array<BuildServiceConfig>;
  serviceProfiles: Array<ServicesProfile>;
  developerVersionsInfo: Array<DeveloperVersionInfo>;
  developerDesiredVersions: Array<DeveloperDesiredVersion>;
  developerDesiredVersionsHistory: Array<TimedDeveloperDesiredVersions>;
  testedVersions?: Maybe<Array<DeveloperDesiredVersion>>;
  clientVersionsInfo: Array<ClientVersionInfo>;
  clientDesiredVersions: Array<ClientDesiredVersion>;
  clientDesiredVersionsHistory: Array<TimedClientDesiredVersions>;
  providersInfo: Array<DistributionProviderInfo>;
  providerDesiredVersions: Array<DeveloperDesiredVersion>;
  providerTestedVersions: Array<DeveloperDesiredVersion>;
  installedDesiredVersions: Array<ClientDesiredVersion>;
  serviceStates: Array<DistributionServiceState>;
  logServices: Array<Scalars['String']>;
  logInstances: Array<Scalars['String']>;
  logDirectories: Array<Scalars['String']>;
  logProcesses: Array<Scalars['String']>;
  logLevels: Array<Scalars['String']>;
  logsStartTime?: Maybe<Scalars['Date']>;
  logsEndTime?: Maybe<Scalars['Date']>;
  logs: Array<SequencedServiceLogLine>;
  faultDistributions: Array<Scalars['String']>;
  faultServices: Array<Scalars['String']>;
  faultsStartTime?: Maybe<Scalars['Date']>;
  faultsEndTime?: Maybe<Scalars['Date']>;
  faults: Array<DistributionFaultReport>;
  taskTypes: Array<Scalars['String']>;
  tasks: Array<TaskInfo>;
};


export type QueryUserAccountsInfoArgs = {
  account?: Maybe<Scalars['String']>;
};


export type QueryServiceAccountsInfoArgs = {
  account?: Maybe<Scalars['String']>;
};


export type QueryConsumerAccountsInfoArgs = {
  account?: Maybe<Scalars['String']>;
};


export type QueryAccessTokenArgs = {
  account: Scalars['String'];
};


export type QueryBuildDeveloperServicesConfigArgs = {
  service?: Maybe<Scalars['String']>;
};


export type QueryBuildClientServicesConfigArgs = {
  service?: Maybe<Scalars['String']>;
};


export type QueryServiceProfilesArgs = {
  profile?: Maybe<Scalars['String']>;
};


export type QueryDeveloperVersionsInfoArgs = {
  service?: Maybe<Scalars['String']>;
  distribution?: Maybe<Scalars['String']>;
  version?: Maybe<DeveloperVersionInput>;
};


export type QueryDeveloperDesiredVersionsArgs = {
  testConsumer?: Maybe<Scalars['String']>;
  services?: Maybe<Array<Scalars['String']>>;
};


export type QueryDeveloperDesiredVersionsHistoryArgs = {
  limit: Scalars['Int'];
};


export type QueryClientVersionsInfoArgs = {
  service?: Maybe<Scalars['String']>;
  distribution?: Maybe<Scalars['String']>;
  version?: Maybe<ClientVersionInput>;
};


export type QueryClientDesiredVersionsArgs = {
  services?: Maybe<Array<Scalars['String']>>;
};


export type QueryClientDesiredVersionsHistoryArgs = {
  limit: Scalars['Int'];
};


export type QueryProvidersInfoArgs = {
  distribution?: Maybe<Scalars['String']>;
};


export type QueryProviderDesiredVersionsArgs = {
  distribution: Scalars['String'];
};


export type QueryProviderTestedVersionsArgs = {
  distribution: Scalars['String'];
};


export type QueryInstalledDesiredVersionsArgs = {
  distribution: Scalars['String'];
  services?: Maybe<Array<Scalars['String']>>;
};


export type QueryServiceStatesArgs = {
  distribution?: Maybe<Scalars['String']>;
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
};


export type QueryLogInstancesArgs = {
  service: Scalars['String'];
};


export type QueryLogDirectoriesArgs = {
  service: Scalars['String'];
  instance: Scalars['String'];
};


export type QueryLogProcessesArgs = {
  service: Scalars['String'];
  instance: Scalars['String'];
  directory: Scalars['String'];
};


export type QueryLogLevelsArgs = {
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
  process?: Maybe<Scalars['String']>;
  task?: Maybe<Scalars['String']>;
};


export type QueryLogsStartTimeArgs = {
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
  process?: Maybe<Scalars['String']>;
  task?: Maybe<Scalars['String']>;
};


export type QueryLogsEndTimeArgs = {
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
  process?: Maybe<Scalars['String']>;
  task?: Maybe<Scalars['String']>;
};


export type QueryLogsArgs = {
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  process?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
  task?: Maybe<Scalars['String']>;
  from?: Maybe<Scalars['BigInt']>;
  to?: Maybe<Scalars['BigInt']>;
  fromTime?: Maybe<Scalars['Date']>;
  toTime?: Maybe<Scalars['Date']>;
  levels?: Maybe<Array<Scalars['String']>>;
  find?: Maybe<Scalars['String']>;
  limit?: Maybe<Scalars['Int']>;
};


export type QueryFaultServicesArgs = {
  distribution?: Maybe<Scalars['String']>;
};


export type QueryFaultsStartTimeArgs = {
  distribution?: Maybe<Scalars['String']>;
  service?: Maybe<Scalars['String']>;
};


export type QueryFaultsEndTimeArgs = {
  distribution?: Maybe<Scalars['String']>;
  service?: Maybe<Scalars['String']>;
};


export type QueryFaultsArgs = {
  fault?: Maybe<Scalars['String']>;
  distribution?: Maybe<Scalars['String']>;
  service?: Maybe<Scalars['String']>;
  fromTime?: Maybe<Scalars['Date']>;
  toTime?: Maybe<Scalars['Date']>;
  limit?: Maybe<Scalars['Int']>;
};


export type QueryTasksArgs = {
  task?: Maybe<Scalars['String']>;
  type?: Maybe<Scalars['String']>;
  parameters?: Maybe<Array<TaskParameterInput>>;
  onlyActive?: Maybe<Scalars['Boolean']>;
  limit?: Maybe<Scalars['Int']>;
};

export type Repository = {
  __typename?: 'Repository';
  name: Scalars['String'];
  git: GitConfig;
  subDirectory?: Maybe<Scalars['String']>;
};

export type RepositoryInput = {
  name: Scalars['String'];
  git: GitConfigInput;
  subDirectory?: Maybe<Scalars['String']>;
};

export type SequencedServiceLogLine = {
  __typename?: 'SequencedServiceLogLine';
  sequence: Scalars['BigInt'];
  instance: Scalars['String'];
  directory: Scalars['String'];
  process: Scalars['String'];
  payload: LogLine;
};

export type ServiceAccountInfo = {
  __typename?: 'ServiceAccountInfo';
  account: Scalars['String'];
  name: Scalars['String'];
  role: AccountRole;
};

export type ServiceFaultReport = {
  __typename?: 'ServiceFaultReport';
  fault: Scalars['String'];
  info: FaultInfo;
  files: Array<FileInfo>;
};

export type ServiceFaultReportInput = {
  fault: Scalars['String'];
  info: FaultInfoInput;
  files: Array<FileInfoInput>;
};

export type ServiceState = {
  __typename?: 'ServiceState';
  time: Scalars['Date'];
  installTime?: Maybe<Scalars['Date']>;
  startTime?: Maybe<Scalars['Date']>;
  version?: Maybe<ClientDistributionVersion>;
  updateToVersion?: Maybe<ClientDistributionVersion>;
  updateError?: Maybe<UpdateError>;
  failuresCount?: Maybe<Scalars['Int']>;
  lastExitCode?: Maybe<Scalars['Int']>;
};

export type ServiceStateInput = {
  time: Scalars['Date'];
  installTime?: Maybe<Scalars['Date']>;
  startTime?: Maybe<Scalars['Date']>;
  version?: Maybe<ClientDistributionVersionInput>;
  updateToVersion?: Maybe<ClientDistributionVersionInput>;
  updateError?: Maybe<UpdateErrorInput>;
  failuresCount?: Maybe<Scalars['Int']>;
  lastExitCode?: Maybe<Scalars['Int']>;
};

export type ServicesProfile = {
  __typename?: 'ServicesProfile';
  profile: Scalars['String'];
  services: Array<Scalars['String']>;
};

export type Subscription = {
  __typename?: 'Subscription';
  subscribeLogs: Array<SequencedServiceLogLine>;
  testSubscription: Scalars['String'];
};


export type SubscriptionSubscribeLogsArgs = {
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
  process?: Maybe<Scalars['String']>;
  task?: Maybe<Scalars['String']>;
  from?: Maybe<Scalars['BigInt']>;
  prefetch?: Maybe<Scalars['Int']>;
  levels?: Maybe<Array<Scalars['String']>>;
};

export type TaskInfo = {
  __typename?: 'TaskInfo';
  task: Scalars['String'];
  type: Scalars['String'];
  parameters: Array<TaskParameter>;
  creationTime: Scalars['Date'];
  active?: Maybe<Scalars['Boolean']>;
};

export type TaskParameter = {
  __typename?: 'TaskParameter';
  name: Scalars['String'];
  value: Scalars['String'];
};

export type TaskParameterInput = {
  name: Scalars['String'];
  value: Scalars['String'];
};

export type TimedClientDesiredVersions = {
  __typename?: 'TimedClientDesiredVersions';
  time: Scalars['Date'];
  author: Scalars['String'];
  versions: Array<ClientDesiredVersion>;
};

export type TimedDeveloperDesiredVersions = {
  __typename?: 'TimedDeveloperDesiredVersions';
  time: Scalars['Date'];
  author: Scalars['String'];
  versions: Array<DeveloperDesiredVersion>;
};

export type UpdateError = {
  __typename?: 'UpdateError';
  critical: Scalars['Boolean'];
  error: Scalars['String'];
};

export type UpdateErrorInput = {
  critical: Scalars['Boolean'];
  error: Scalars['String'];
};

export type UserAccountInfo = {
  __typename?: 'UserAccountInfo';
  account: Scalars['String'];
  name: Scalars['String'];
  role: AccountRole;
  properties: UserAccountProperties;
};

export type UserAccountProperties = {
  __typename?: 'UserAccountProperties';
  email?: Maybe<Scalars['String']>;
  notifications: Array<Scalars['String']>;
};

export type UserAccountPropertiesInput = {
  email?: Maybe<Scalars['String']>;
  notifications: Array<Scalars['String']>;
};

export type LoginMutationVariables = Exact<{
  account: Scalars['String'];
  password: Scalars['String'];
}>;


export type LoginMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'login'>
);

export type WhoAmIQueryVariables = Exact<{ [key: string]: never; }>;


export type WhoAmIQuery = (
  { __typename?: 'Query' }
  & { whoAmI: (
    { __typename?: 'UserAccountInfo' }
    & Pick<UserAccountInfo, 'account' | 'name' | 'role'>
  ) }
);

export type DistributionInfoQueryVariables = Exact<{ [key: string]: never; }>;


export type DistributionInfoQuery = (
  { __typename?: 'Query' }
  & { distributionInfo: (
    { __typename?: 'DistributionInfo' }
    & Pick<DistributionInfo, 'distribution' | 'title'>
  ) }
);

export type DeveloperVersionsInfoQueryVariables = Exact<{
  service?: Maybe<Scalars['String']>;
  version?: Maybe<DeveloperVersionInput>;
}>;


export type DeveloperVersionsInfoQuery = (
  { __typename?: 'Query' }
  & { developerVersionsInfo: Array<(
    { __typename?: 'DeveloperVersionInfo' }
    & Pick<DeveloperVersionInfo, 'service'>
    & { version: (
      { __typename?: 'DeveloperDistributionVersion' }
      & Pick<DeveloperDistributionVersion, 'distribution' | 'build'>
    ), buildInfo: (
      { __typename?: 'BuildInfo' }
      & Pick<BuildInfo, 'author' | 'time' | 'comment'>
      & { sources: Array<(
        { __typename?: 'Repository' }
        & Pick<Repository, 'name'>
        & { git: (
          { __typename?: 'GitConfig' }
          & Pick<GitConfig, 'url' | 'branch' | 'cloneSubmodules'>
        ) }
      )> }
    ) }
  )> }
);

export type DeveloperDesiredVersionsQueryVariables = Exact<{ [key: string]: never; }>;


export type DeveloperDesiredVersionsQuery = (
  { __typename?: 'Query' }
  & { developerDesiredVersions: Array<(
    { __typename?: 'DeveloperDesiredVersion' }
    & Pick<DeveloperDesiredVersion, 'service'>
    & { version: (
      { __typename?: 'DeveloperDistributionVersion' }
      & Pick<DeveloperDistributionVersion, 'distribution' | 'build'>
    ) }
  )> }
);

export type DeveloperDesiredVersionsHistoryQueryVariables = Exact<{
  limit: Scalars['Int'];
}>;


export type DeveloperDesiredVersionsHistoryQuery = (
  { __typename?: 'Query' }
  & { developerDesiredVersionsHistory: Array<(
    { __typename?: 'TimedDeveloperDesiredVersions' }
    & Pick<TimedDeveloperDesiredVersions, 'time' | 'author'>
    & { versions: Array<(
      { __typename?: 'DeveloperDesiredVersion' }
      & Pick<DeveloperDesiredVersion, 'service'>
      & { version: (
        { __typename?: 'DeveloperDistributionVersion' }
        & Pick<DeveloperDistributionVersion, 'distribution' | 'build'>
      ) }
    )> }
  )> }
);

export type ProviderDesiredVersionsQueryVariables = Exact<{
  distribution: Scalars['String'];
}>;


export type ProviderDesiredVersionsQuery = (
  { __typename?: 'Query' }
  & { providerDesiredVersions: Array<(
    { __typename?: 'DeveloperDesiredVersion' }
    & Pick<DeveloperDesiredVersion, 'service'>
    & { version: (
      { __typename?: 'DeveloperDistributionVersion' }
      & Pick<DeveloperDistributionVersion, 'distribution' | 'build'>
    ) }
  )> }
);

export type BuildDeveloperVersionMutationVariables = Exact<{
  service: Scalars['String'];
  version: DeveloperVersionInput;
  comment: Scalars['String'];
  buildClientVersion: Scalars['Boolean'];
}>;


export type BuildDeveloperVersionMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'buildDeveloperVersion'>
);

export type AddDeveloperVersionInfoMutationVariables = Exact<{
  info: DeveloperVersionInfoInput;
}>;


export type AddDeveloperVersionInfoMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'addDeveloperVersionInfo'>
);

export type RemoveDeveloperVersionMutationVariables = Exact<{
  service: Scalars['String'];
  version: DeveloperDistributionVersionInput;
}>;


export type RemoveDeveloperVersionMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'removeDeveloperVersion'>
);

export type SetDeveloperDesiredVersionsMutationVariables = Exact<{
  versions: Array<DeveloperDesiredVersionDeltaInput> | DeveloperDesiredVersionDeltaInput;
}>;


export type SetDeveloperDesiredVersionsMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'setDeveloperDesiredVersions'>
);

export type SetTestedVersionsMutationVariables = Exact<{
  versions: Array<DeveloperDesiredVersionInput> | DeveloperDesiredVersionInput;
}>;


export type SetTestedVersionsMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'setTestedVersions'>
);

export type TestedVersionsQueryVariables = Exact<{ [key: string]: never; }>;


export type TestedVersionsQuery = (
  { __typename?: 'Query' }
  & { testedVersions?: Maybe<Array<(
    { __typename?: 'DeveloperDesiredVersion' }
    & Pick<DeveloperDesiredVersion, 'service'>
    & { version: (
      { __typename?: 'DeveloperDistributionVersion' }
      & Pick<DeveloperDistributionVersion, 'distribution' | 'build'>
    ) }
  )>> }
);

export type SetProviderTestedVersionsMutationVariables = Exact<{
  distribution: Scalars['String'];
  versions: Array<DeveloperDesiredVersionInput> | DeveloperDesiredVersionInput;
}>;


export type SetProviderTestedVersionsMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'setProviderTestedVersions'>
);

export type ProviderTestedVersionsQueryVariables = Exact<{
  distribution: Scalars['String'];
}>;


export type ProviderTestedVersionsQuery = (
  { __typename?: 'Query' }
  & { providerTestedVersions: Array<(
    { __typename?: 'DeveloperDesiredVersion' }
    & Pick<DeveloperDesiredVersion, 'service'>
    & { version: (
      { __typename?: 'DeveloperDistributionVersion' }
      & Pick<DeveloperDistributionVersion, 'distribution' | 'build'>
    ) }
  )> }
);

export type ClientVersionsInfoQueryVariables = Exact<{
  service?: Maybe<Scalars['String']>;
  version?: Maybe<ClientVersionInput>;
}>;


export type ClientVersionsInfoQuery = (
  { __typename?: 'Query' }
  & { clientVersionsInfo: Array<(
    { __typename?: 'ClientVersionInfo' }
    & Pick<ClientVersionInfo, 'service'>
    & { version: (
      { __typename?: 'ClientDistributionVersion' }
      & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
    ), buildInfo: (
      { __typename?: 'BuildInfo' }
      & Pick<BuildInfo, 'author' | 'time' | 'comment'>
      & { sources: Array<(
        { __typename?: 'Repository' }
        & Pick<Repository, 'name'>
        & { git: (
          { __typename?: 'GitConfig' }
          & Pick<GitConfig, 'url' | 'branch' | 'cloneSubmodules'>
        ) }
      )> }
    ), installInfo: (
      { __typename?: 'InstallInfo' }
      & Pick<InstallInfo, 'account' | 'time'>
    ) }
  )> }
);

export type ClientDesiredVersionsQueryVariables = Exact<{ [key: string]: never; }>;


export type ClientDesiredVersionsQuery = (
  { __typename?: 'Query' }
  & { clientDesiredVersions: Array<(
    { __typename?: 'ClientDesiredVersion' }
    & Pick<ClientDesiredVersion, 'service'>
    & { version: (
      { __typename?: 'ClientDistributionVersion' }
      & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
    ) }
  )> }
);

export type ClientDesiredVersionsHistoryQueryVariables = Exact<{
  limit: Scalars['Int'];
}>;


export type ClientDesiredVersionsHistoryQuery = (
  { __typename?: 'Query' }
  & { clientDesiredVersionsHistory: Array<(
    { __typename?: 'TimedClientDesiredVersions' }
    & Pick<TimedClientDesiredVersions, 'time' | 'author'>
    & { versions: Array<(
      { __typename?: 'ClientDesiredVersion' }
      & Pick<ClientDesiredVersion, 'service'>
      & { version: (
        { __typename?: 'ClientDistributionVersion' }
        & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
      ) }
    )> }
  )> }
);

export type InstalledDesiredVersionsQueryVariables = Exact<{
  distribution: Scalars['String'];
}>;


export type InstalledDesiredVersionsQuery = (
  { __typename?: 'Query' }
  & { installedDesiredVersions: Array<(
    { __typename?: 'ClientDesiredVersion' }
    & Pick<ClientDesiredVersion, 'service'>
    & { version: (
      { __typename?: 'ClientDistributionVersion' }
      & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
    ) }
  )> }
);

export type BuildClientVersionsMutationVariables = Exact<{
  versions: Array<DeveloperDesiredVersionInput> | DeveloperDesiredVersionInput;
}>;


export type BuildClientVersionsMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'buildClientVersions'>
);

export type AddClientVersionInfoMutationVariables = Exact<{
  info: ClientVersionInfoInput;
}>;


export type AddClientVersionInfoMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'addClientVersionInfo'>
);

export type RemoveClientVersionMutationVariables = Exact<{
  service: Scalars['String'];
  version: ClientDistributionVersionInput;
}>;


export type RemoveClientVersionMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'removeClientVersion'>
);

export type SetClientDesiredVersionsMutationVariables = Exact<{
  versions: Array<ClientDesiredVersionDeltaInput> | ClientDesiredVersionDeltaInput;
}>;


export type SetClientDesiredVersionsMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'setClientDesiredVersions'>
);

export type ServiceStatesQueryVariables = Exact<{
  distribution: Scalars['String'];
}>;


export type ServiceStatesQuery = (
  { __typename?: 'Query' }
  & { serviceStates: Array<(
    { __typename?: 'DistributionServiceState' }
    & Pick<DistributionServiceState, 'distribution'>
    & { payload: (
      { __typename?: 'InstanceServiceState' }
      & Pick<InstanceServiceState, 'instance' | 'service' | 'directory'>
      & { state: (
        { __typename?: 'ServiceState' }
        & Pick<ServiceState, 'time' | 'installTime' | 'startTime' | 'failuresCount' | 'lastExitCode'>
        & { version?: Maybe<(
          { __typename?: 'ClientDistributionVersion' }
          & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
        )>, updateToVersion?: Maybe<(
          { __typename?: 'ClientDistributionVersion' }
          & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
        )>, updateError?: Maybe<(
          { __typename?: 'UpdateError' }
          & Pick<UpdateError, 'critical' | 'error'>
        )> }
      ) }
    ) }
  )> }
);

export type LogServicesQueryVariables = Exact<{ [key: string]: never; }>;


export type LogServicesQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'logServices'>
);

export type LogInstancesQueryVariables = Exact<{
  service: Scalars['String'];
}>;


export type LogInstancesQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'logInstances'>
);

export type LogDirectoriesQueryVariables = Exact<{
  service: Scalars['String'];
  instance: Scalars['String'];
}>;


export type LogDirectoriesQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'logDirectories'>
);

export type LogProcessesQueryVariables = Exact<{
  service: Scalars['String'];
  instance: Scalars['String'];
  directory: Scalars['String'];
}>;


export type LogProcessesQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'logProcesses'>
);

export type LogLevelsQueryVariables = Exact<{
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
  process?: Maybe<Scalars['String']>;
  task?: Maybe<Scalars['String']>;
}>;


export type LogLevelsQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'logLevels'>
);

export type LogsStartTimeQueryVariables = Exact<{
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
  process?: Maybe<Scalars['String']>;
  task?: Maybe<Scalars['String']>;
}>;


export type LogsStartTimeQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'logsStartTime'>
);

export type LogsEndTimeQueryVariables = Exact<{
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
  process?: Maybe<Scalars['String']>;
  task?: Maybe<Scalars['String']>;
}>;


export type LogsEndTimeQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'logsEndTime'>
);

export type TaskLogsQueryVariables = Exact<{
  task: Scalars['String'];
  from?: Maybe<Scalars['BigInt']>;
  to?: Maybe<Scalars['BigInt']>;
  fromTime?: Maybe<Scalars['Date']>;
  toTime?: Maybe<Scalars['Date']>;
  levels?: Maybe<Array<Scalars['String']> | Scalars['String']>;
  find?: Maybe<Scalars['String']>;
  limit?: Maybe<Scalars['Int']>;
}>;


export type TaskLogsQuery = (
  { __typename?: 'Query' }
  & { logs: Array<(
    { __typename?: 'SequencedServiceLogLine' }
    & Pick<SequencedServiceLogLine, 'sequence' | 'instance' | 'directory' | 'process'>
    & { payload: (
      { __typename?: 'LogLine' }
      & Pick<LogLine, 'time' | 'level' | 'unit' | 'message' | 'terminationStatus'>
    ) }
  )> }
);

export type ServiceLogsQueryVariables = Exact<{
  service: Scalars['String'];
  from?: Maybe<Scalars['BigInt']>;
  to?: Maybe<Scalars['BigInt']>;
  fromTime?: Maybe<Scalars['Date']>;
  toTime?: Maybe<Scalars['Date']>;
  levels?: Maybe<Array<Scalars['String']> | Scalars['String']>;
  find?: Maybe<Scalars['String']>;
  limit?: Maybe<Scalars['Int']>;
}>;


export type ServiceLogsQuery = (
  { __typename?: 'Query' }
  & { logs: Array<(
    { __typename?: 'SequencedServiceLogLine' }
    & Pick<SequencedServiceLogLine, 'sequence' | 'instance' | 'directory' | 'process'>
    & { payload: (
      { __typename?: 'LogLine' }
      & Pick<LogLine, 'time' | 'level' | 'unit' | 'message' | 'terminationStatus'>
    ) }
  )> }
);

export type InstanceLogsQueryVariables = Exact<{
  service: Scalars['String'];
  instance: Scalars['String'];
  from?: Maybe<Scalars['BigInt']>;
  to?: Maybe<Scalars['BigInt']>;
  fromTime?: Maybe<Scalars['Date']>;
  toTime?: Maybe<Scalars['Date']>;
  levels?: Maybe<Array<Scalars['String']> | Scalars['String']>;
  find?: Maybe<Scalars['String']>;
  limit?: Maybe<Scalars['Int']>;
}>;


export type InstanceLogsQuery = (
  { __typename?: 'Query' }
  & { logs: Array<(
    { __typename?: 'SequencedServiceLogLine' }
    & Pick<SequencedServiceLogLine, 'sequence' | 'directory' | 'process'>
    & { payload: (
      { __typename?: 'LogLine' }
      & Pick<LogLine, 'time' | 'level' | 'unit' | 'message' | 'terminationStatus'>
    ) }
  )> }
);

export type DirectoryLogsQueryVariables = Exact<{
  service: Scalars['String'];
  instance: Scalars['String'];
  directory: Scalars['String'];
  from?: Maybe<Scalars['BigInt']>;
  to?: Maybe<Scalars['BigInt']>;
  fromTime?: Maybe<Scalars['Date']>;
  toTime?: Maybe<Scalars['Date']>;
  levels?: Maybe<Array<Scalars['String']> | Scalars['String']>;
  find?: Maybe<Scalars['String']>;
  limit?: Maybe<Scalars['Int']>;
}>;


export type DirectoryLogsQuery = (
  { __typename?: 'Query' }
  & { logs: Array<(
    { __typename?: 'SequencedServiceLogLine' }
    & Pick<SequencedServiceLogLine, 'sequence' | 'process'>
    & { payload: (
      { __typename?: 'LogLine' }
      & Pick<LogLine, 'time' | 'level' | 'unit' | 'message' | 'terminationStatus'>
    ) }
  )> }
);

export type ProcessLogsQueryVariables = Exact<{
  service: Scalars['String'];
  instance: Scalars['String'];
  directory: Scalars['String'];
  process: Scalars['String'];
  from?: Maybe<Scalars['BigInt']>;
  to?: Maybe<Scalars['BigInt']>;
  fromTime?: Maybe<Scalars['Date']>;
  toTime?: Maybe<Scalars['Date']>;
  levels?: Maybe<Array<Scalars['String']> | Scalars['String']>;
  find?: Maybe<Scalars['String']>;
  limit?: Maybe<Scalars['Int']>;
}>;


export type ProcessLogsQuery = (
  { __typename?: 'Query' }
  & { logs: Array<(
    { __typename?: 'SequencedServiceLogLine' }
    & Pick<SequencedServiceLogLine, 'sequence'>
    & { payload: (
      { __typename?: 'LogLine' }
      & Pick<LogLine, 'time' | 'level' | 'unit' | 'message' | 'terminationStatus'>
    ) }
  )> }
);

export type FaultDistributionsQueryVariables = Exact<{ [key: string]: never; }>;


export type FaultDistributionsQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'faultDistributions'>
);

export type FaultServicesQueryVariables = Exact<{
  distribution?: Maybe<Scalars['String']>;
}>;


export type FaultServicesQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'faultServices'>
);

export type FaultsStartTimeQueryVariables = Exact<{
  distribution?: Maybe<Scalars['String']>;
  service?: Maybe<Scalars['String']>;
}>;


export type FaultsStartTimeQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'faultsStartTime'>
);

export type FaultsEndTimeQueryVariables = Exact<{
  distribution?: Maybe<Scalars['String']>;
  service?: Maybe<Scalars['String']>;
}>;


export type FaultsEndTimeQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'faultsEndTime'>
);

export type FaultsQueryVariables = Exact<{
  distribution?: Maybe<Scalars['String']>;
  service?: Maybe<Scalars['String']>;
  fromTime?: Maybe<Scalars['Date']>;
  toTime?: Maybe<Scalars['Date']>;
}>;


export type FaultsQuery = (
  { __typename?: 'Query' }
  & { faults: Array<(
    { __typename?: 'DistributionFaultReport' }
    & Pick<DistributionFaultReport, 'distribution'>
    & { payload: (
      { __typename?: 'ServiceFaultReport' }
      & Pick<ServiceFaultReport, 'fault'>
      & { info: (
        { __typename?: 'FaultInfo' }
        & Pick<FaultInfo, 'time' | 'instance' | 'service' | 'serviceDirectory' | 'serviceRole'>
        & { state: (
          { __typename?: 'ServiceState' }
          & Pick<ServiceState, 'time' | 'installTime' | 'startTime' | 'failuresCount' | 'lastExitCode'>
          & { version?: Maybe<(
            { __typename?: 'ClientDistributionVersion' }
            & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
          )>, updateToVersion?: Maybe<(
            { __typename?: 'ClientDistributionVersion' }
            & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
          )>, updateError?: Maybe<(
            { __typename?: 'UpdateError' }
            & Pick<UpdateError, 'critical' | 'error'>
          )> }
        ), logTail: Array<(
          { __typename?: 'LogLine' }
          & Pick<LogLine, 'time' | 'level' | 'unit' | 'message' | 'terminationStatus'>
        )> }
      ), files: Array<(
        { __typename?: 'FileInfo' }
        & Pick<FileInfo, 'path' | 'time' | 'length'>
      )> }
    ) }
  )> }
);

export type FaultQueryVariables = Exact<{
  fault: Scalars['String'];
}>;


export type FaultQuery = (
  { __typename?: 'Query' }
  & { faults: Array<(
    { __typename?: 'DistributionFaultReport' }
    & Pick<DistributionFaultReport, 'distribution'>
    & { payload: (
      { __typename?: 'ServiceFaultReport' }
      & Pick<ServiceFaultReport, 'fault'>
      & { info: (
        { __typename?: 'FaultInfo' }
        & Pick<FaultInfo, 'time' | 'instance' | 'service' | 'serviceDirectory' | 'serviceRole'>
        & { state: (
          { __typename?: 'ServiceState' }
          & Pick<ServiceState, 'time' | 'installTime' | 'startTime' | 'failuresCount' | 'lastExitCode'>
          & { version?: Maybe<(
            { __typename?: 'ClientDistributionVersion' }
            & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
          )>, updateToVersion?: Maybe<(
            { __typename?: 'ClientDistributionVersion' }
            & Pick<ClientDistributionVersion, 'distribution' | 'developerBuild' | 'clientBuild'>
          )>, updateError?: Maybe<(
            { __typename?: 'UpdateError' }
            & Pick<UpdateError, 'critical' | 'error'>
          )> }
        ), logTail: Array<(
          { __typename?: 'LogLine' }
          & Pick<LogLine, 'time' | 'level' | 'unit' | 'message' | 'terminationStatus'>
        )> }
      ), files: Array<(
        { __typename?: 'FileInfo' }
        & Pick<FileInfo, 'path' | 'time' | 'length'>
      )> }
    ) }
  )> }
);

export type AccountsListQueryVariables = Exact<{ [key: string]: never; }>;


export type AccountsListQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'accountsList'>
);

export type UserAccountsInfoQueryVariables = Exact<{ [key: string]: never; }>;


export type UserAccountsInfoQuery = (
  { __typename?: 'Query' }
  & { userAccountsInfo: Array<(
    { __typename?: 'UserAccountInfo' }
    & Pick<UserAccountInfo, 'account' | 'name' | 'role'>
    & { properties: (
      { __typename?: 'UserAccountProperties' }
      & Pick<UserAccountProperties, 'email' | 'notifications'>
    ) }
  )> }
);

export type UserAccountInfoQueryVariables = Exact<{
  account: Scalars['String'];
}>;


export type UserAccountInfoQuery = (
  { __typename?: 'Query' }
  & { userAccountsInfo: Array<(
    { __typename?: 'UserAccountInfo' }
    & Pick<UserAccountInfo, 'account' | 'name' | 'role'>
    & { properties: (
      { __typename?: 'UserAccountProperties' }
      & Pick<UserAccountProperties, 'email' | 'notifications'>
    ) }
  )> }
);

export type ServiceAccountsInfoQueryVariables = Exact<{ [key: string]: never; }>;


export type ServiceAccountsInfoQuery = (
  { __typename?: 'Query' }
  & { serviceAccountsInfo: Array<(
    { __typename?: 'ServiceAccountInfo' }
    & Pick<ServiceAccountInfo, 'account' | 'name' | 'role'>
  )> }
);

export type ServiceAccountInfoQueryVariables = Exact<{
  account: Scalars['String'];
}>;


export type ServiceAccountInfoQuery = (
  { __typename?: 'Query' }
  & { serviceAccountsInfo: Array<(
    { __typename?: 'ServiceAccountInfo' }
    & Pick<ServiceAccountInfo, 'account' | 'name' | 'role'>
  )> }
);

export type ConsumerAccountsInfoQueryVariables = Exact<{ [key: string]: never; }>;


export type ConsumerAccountsInfoQuery = (
  { __typename?: 'Query' }
  & { consumerAccountsInfo: Array<(
    { __typename?: 'ConsumerAccountInfo' }
    & Pick<ConsumerAccountInfo, 'account' | 'name' | 'role'>
    & { properties: (
      { __typename?: 'ConsumerAccountProperties' }
      & Pick<ConsumerAccountProperties, 'url' | 'profile'>
    ) }
  )> }
);

export type ConsumerAccountInfoQueryVariables = Exact<{
  account: Scalars['String'];
}>;


export type ConsumerAccountInfoQuery = (
  { __typename?: 'Query' }
  & { consumerAccountsInfo: Array<(
    { __typename?: 'ConsumerAccountInfo' }
    & Pick<ConsumerAccountInfo, 'account' | 'name' | 'role'>
    & { properties: (
      { __typename?: 'ConsumerAccountProperties' }
      & Pick<ConsumerAccountProperties, 'url' | 'profile'>
    ) }
  )> }
);

export type AccessTokenQueryVariables = Exact<{
  account: Scalars['String'];
}>;


export type AccessTokenQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'accessToken'>
);

export type AddUserAccountMutationVariables = Exact<{
  account: Scalars['String'];
  name: Scalars['String'];
  role: AccountRole;
  password: Scalars['String'];
  properties: UserAccountPropertiesInput;
}>;


export type AddUserAccountMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'addUserAccount'>
);

export type ChangeUserAccountMutationVariables = Exact<{
  account: Scalars['String'];
  name?: Maybe<Scalars['String']>;
  role: AccountRole;
  oldPassword?: Maybe<Scalars['String']>;
  password?: Maybe<Scalars['String']>;
  properties?: Maybe<UserAccountPropertiesInput>;
}>;


export type ChangeUserAccountMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'changeUserAccount'>
);

export type AddServiceAccountMutationVariables = Exact<{
  account: Scalars['String'];
  name: Scalars['String'];
  role: AccountRole;
}>;


export type AddServiceAccountMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'addServiceAccount'>
);

export type ChangeServiceAccountMutationVariables = Exact<{
  account: Scalars['String'];
  name?: Maybe<Scalars['String']>;
  role: AccountRole;
}>;


export type ChangeServiceAccountMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'changeServiceAccount'>
);

export type AddConsumerAccountMutationVariables = Exact<{
  account: Scalars['String'];
  name: Scalars['String'];
  properties: ConsumerAccountPropertiesInput;
}>;


export type AddConsumerAccountMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'addConsumerAccount'>
);

export type ChangeConsumerAccountMutationVariables = Exact<{
  account: Scalars['String'];
  name?: Maybe<Scalars['String']>;
  properties?: Maybe<ConsumerAccountPropertiesInput>;
}>;


export type ChangeConsumerAccountMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'changeConsumerAccount'>
);

export type RemoveAccountMutationVariables = Exact<{
  account: Scalars['String'];
}>;


export type RemoveAccountMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'removeAccount'>
);

export type BuildDeveloperServicesQueryVariables = Exact<{ [key: string]: never; }>;


export type BuildDeveloperServicesQuery = (
  { __typename?: 'Query' }
  & { buildDeveloperServicesConfig: Array<(
    { __typename?: 'BuildServiceConfig' }
    & Pick<BuildServiceConfig, 'service'>
  )> }
);

export type BuildDeveloperServiceConfigQueryVariables = Exact<{
  service: Scalars['String'];
}>;


export type BuildDeveloperServiceConfigQuery = (
  { __typename?: 'Query' }
  & { buildDeveloperServicesConfig: Array<(
    { __typename?: 'BuildServiceConfig' }
    & Pick<BuildServiceConfig, 'service' | 'distribution'>
    & { environment: Array<(
      { __typename?: 'NamedStringValue' }
      & Pick<NamedStringValue, 'name' | 'value'>
    )>, repositories: Array<(
      { __typename?: 'Repository' }
      & Pick<Repository, 'name' | 'subDirectory'>
      & { git: (
        { __typename?: 'GitConfig' }
        & Pick<GitConfig, 'url' | 'branch' | 'cloneSubmodules'>
      ) }
    )>, privateFiles: Array<(
      { __typename?: 'FileInfo' }
      & Pick<FileInfo, 'path' | 'time' | 'length'>
    )>, macroValues: Array<(
      { __typename?: 'NamedStringValue' }
      & Pick<NamedStringValue, 'name' | 'value'>
    )> }
  )> }
);

export type BuildClientServicesQueryVariables = Exact<{ [key: string]: never; }>;


export type BuildClientServicesQuery = (
  { __typename?: 'Query' }
  & { buildClientServicesConfig: Array<(
    { __typename?: 'BuildServiceConfig' }
    & Pick<BuildServiceConfig, 'service'>
  )> }
);

export type BuildClientServiceConfigQueryVariables = Exact<{
  service: Scalars['String'];
}>;


export type BuildClientServiceConfigQuery = (
  { __typename?: 'Query' }
  & { buildClientServicesConfig: Array<(
    { __typename?: 'BuildServiceConfig' }
    & Pick<BuildServiceConfig, 'service' | 'distribution'>
    & { environment: Array<(
      { __typename?: 'NamedStringValue' }
      & Pick<NamedStringValue, 'name' | 'value'>
    )>, repositories: Array<(
      { __typename?: 'Repository' }
      & Pick<Repository, 'name' | 'subDirectory'>
      & { git: (
        { __typename?: 'GitConfig' }
        & Pick<GitConfig, 'url' | 'branch' | 'cloneSubmodules'>
      ) }
    )>, privateFiles: Array<(
      { __typename?: 'FileInfo' }
      & Pick<FileInfo, 'path' | 'time' | 'length'>
    )>, macroValues: Array<(
      { __typename?: 'NamedStringValue' }
      & Pick<NamedStringValue, 'name' | 'value'>
    )> }
  )> }
);

export type SetBuildDeveloperServiceConfigMutationVariables = Exact<{
  service: Scalars['String'];
  distribution?: Maybe<Scalars['String']>;
  environment: Array<NamedStringValueInput> | NamedStringValueInput;
  repositories: Array<RepositoryInput> | RepositoryInput;
  privateFiles: Array<FileInfoInput> | FileInfoInput;
  macroValues: Array<NamedStringValueInput> | NamedStringValueInput;
}>;


export type SetBuildDeveloperServiceConfigMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'setBuildDeveloperServiceConfig'>
);

export type RemoveBuildDeveloperServiceConfigMutationVariables = Exact<{
  service: Scalars['String'];
}>;


export type RemoveBuildDeveloperServiceConfigMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'removeBuildDeveloperServiceConfig'>
);

export type SetBuildClientServiceConfigMutationVariables = Exact<{
  service: Scalars['String'];
  distribution?: Maybe<Scalars['String']>;
  environment: Array<NamedStringValueInput> | NamedStringValueInput;
  repositories: Array<RepositoryInput> | RepositoryInput;
  privateFiles: Array<FileInfoInput> | FileInfoInput;
  macroValues: Array<NamedStringValueInput> | NamedStringValueInput;
}>;


export type SetBuildClientServiceConfigMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'setBuildClientServiceConfig'>
);

export type RemoveBuildClientServiceConfigMutationVariables = Exact<{
  service: Scalars['String'];
}>;


export type RemoveBuildClientServiceConfigMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'removeBuildClientServiceConfig'>
);

export type ServiceProfilesQueryVariables = Exact<{ [key: string]: never; }>;


export type ServiceProfilesQuery = (
  { __typename?: 'Query' }
  & { serviceProfiles: Array<(
    { __typename?: 'ServicesProfile' }
    & Pick<ServicesProfile, 'profile'>
  )> }
);

export type ProfileServicesQueryVariables = Exact<{
  profile: Scalars['String'];
}>;


export type ProfileServicesQuery = (
  { __typename?: 'Query' }
  & { serviceProfiles: Array<(
    { __typename?: 'ServicesProfile' }
    & Pick<ServicesProfile, 'services'>
  )> }
);

export type AddServicesProfileMutationVariables = Exact<{
  profile: Scalars['String'];
  services: Array<Scalars['String']> | Scalars['String'];
}>;


export type AddServicesProfileMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'addServicesProfile'>
);

export type ChangeServicesProfileMutationVariables = Exact<{
  profile: Scalars['String'];
  services: Array<Scalars['String']> | Scalars['String'];
}>;


export type ChangeServicesProfileMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'changeServicesProfile'>
);

export type RemoveServicesProfileMutationVariables = Exact<{
  profile: Scalars['String'];
}>;


export type RemoveServicesProfileMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'removeServicesProfile'>
);

export type ProvidersInfoQueryVariables = Exact<{ [key: string]: never; }>;


export type ProvidersInfoQuery = (
  { __typename?: 'Query' }
  & { providersInfo: Array<(
    { __typename?: 'DistributionProviderInfo' }
    & Pick<DistributionProviderInfo, 'distribution' | 'url' | 'accessToken' | 'testConsumer' | 'uploadState' | 'autoUpdate'>
  )> }
);

export type AddProviderMutationVariables = Exact<{
  distribution: Scalars['String'];
  url: Scalars['String'];
  accessToken: Scalars['String'];
  testConsumer?: Maybe<Scalars['String']>;
  uploadState: Scalars['Boolean'];
  autoUpdate: Scalars['Boolean'];
}>;


export type AddProviderMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'addProvider'>
);

export type ChangeProviderMutationVariables = Exact<{
  distribution: Scalars['String'];
  url: Scalars['String'];
  accessToken: Scalars['String'];
  testConsumer?: Maybe<Scalars['String']>;
  uploadState: Scalars['Boolean'];
  autoUpdate: Scalars['Boolean'];
}>;


export type ChangeProviderMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'changeProvider'>
);

export type RemoveProviderMutationVariables = Exact<{
  distribution: Scalars['String'];
}>;


export type RemoveProviderMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'removeProvider'>
);

export type SubscribeLogsSubscriptionVariables = Exact<{
  service?: Maybe<Scalars['String']>;
  instance?: Maybe<Scalars['String']>;
  process?: Maybe<Scalars['String']>;
  directory?: Maybe<Scalars['String']>;
  task?: Maybe<Scalars['String']>;
  prefetch: Scalars['Int'];
  levels?: Maybe<Array<Scalars['String']> | Scalars['String']>;
}>;


export type SubscribeLogsSubscription = (
  { __typename?: 'Subscription' }
  & { subscribeLogs: Array<(
    { __typename?: 'SequencedServiceLogLine' }
    & Pick<SequencedServiceLogLine, 'sequence'>
    & { payload: (
      { __typename?: 'LogLine' }
      & Pick<LogLine, 'time' | 'level' | 'unit' | 'message' | 'terminationStatus'>
    ) }
  )> }
);

export type TaskTypesQueryVariables = Exact<{ [key: string]: never; }>;


export type TaskTypesQuery = (
  { __typename?: 'Query' }
  & Pick<Query, 'taskTypes'>
);

export type TasksQueryVariables = Exact<{
  task?: Maybe<Scalars['String']>;
  type?: Maybe<Scalars['String']>;
  parameters?: Maybe<Array<TaskParameterInput> | TaskParameterInput>;
  onlyActive?: Maybe<Scalars['Boolean']>;
  limit?: Maybe<Scalars['Int']>;
}>;


export type TasksQuery = (
  { __typename?: 'Query' }
  & { tasks: Array<(
    { __typename?: 'TaskInfo' }
    & Pick<TaskInfo, 'task' | 'type' | 'creationTime' | 'active'>
    & { parameters: Array<(
      { __typename?: 'TaskParameter' }
      & Pick<TaskParameter, 'name' | 'value'>
    )> }
  )> }
);

export type CancelTaskMutationVariables = Exact<{
  task: Scalars['String'];
}>;


export type CancelTaskMutation = (
  { __typename?: 'Mutation' }
  & Pick<Mutation, 'cancelTask'>
);


export const LoginDocument = gql`
    mutation login($account: String!, $password: String!) {
  login(account: $account, password: $password)
}
    `;
export type LoginMutationFn = Apollo.MutationFunction<LoginMutation, LoginMutationVariables>;

/**
 * __useLoginMutation__
 *
 * To run a mutation, you first call `useLoginMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useLoginMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [loginMutation, { data, loading, error }] = useLoginMutation({
 *   variables: {
 *      account: // value for 'account'
 *      password: // value for 'password'
 *   },
 * });
 */
export function useLoginMutation(baseOptions?: Apollo.MutationHookOptions<LoginMutation, LoginMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<LoginMutation, LoginMutationVariables>(LoginDocument, options);
      }
export type LoginMutationHookResult = ReturnType<typeof useLoginMutation>;
export type LoginMutationResult = Apollo.MutationResult<LoginMutation>;
export type LoginMutationOptions = Apollo.BaseMutationOptions<LoginMutation, LoginMutationVariables>;
export const WhoAmIDocument = gql`
    query whoAmI {
  whoAmI {
    account
    name
    role
  }
}
    `;

/**
 * __useWhoAmIQuery__
 *
 * To run a query within a React component, call `useWhoAmIQuery` and pass it any options that fit your needs.
 * When your component renders, `useWhoAmIQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useWhoAmIQuery({
 *   variables: {
 *   },
 * });
 */
export function useWhoAmIQuery(baseOptions?: Apollo.QueryHookOptions<WhoAmIQuery, WhoAmIQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<WhoAmIQuery, WhoAmIQueryVariables>(WhoAmIDocument, options);
      }
export function useWhoAmILazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<WhoAmIQuery, WhoAmIQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<WhoAmIQuery, WhoAmIQueryVariables>(WhoAmIDocument, options);
        }
export type WhoAmIQueryHookResult = ReturnType<typeof useWhoAmIQuery>;
export type WhoAmILazyQueryHookResult = ReturnType<typeof useWhoAmILazyQuery>;
export type WhoAmIQueryResult = Apollo.QueryResult<WhoAmIQuery, WhoAmIQueryVariables>;
export const DistributionInfoDocument = gql`
    query distributionInfo {
  distributionInfo {
    distribution
    title
  }
}
    `;

/**
 * __useDistributionInfoQuery__
 *
 * To run a query within a React component, call `useDistributionInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useDistributionInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useDistributionInfoQuery({
 *   variables: {
 *   },
 * });
 */
export function useDistributionInfoQuery(baseOptions?: Apollo.QueryHookOptions<DistributionInfoQuery, DistributionInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<DistributionInfoQuery, DistributionInfoQueryVariables>(DistributionInfoDocument, options);
      }
export function useDistributionInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<DistributionInfoQuery, DistributionInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<DistributionInfoQuery, DistributionInfoQueryVariables>(DistributionInfoDocument, options);
        }
export type DistributionInfoQueryHookResult = ReturnType<typeof useDistributionInfoQuery>;
export type DistributionInfoLazyQueryHookResult = ReturnType<typeof useDistributionInfoLazyQuery>;
export type DistributionInfoQueryResult = Apollo.QueryResult<DistributionInfoQuery, DistributionInfoQueryVariables>;
export const DeveloperVersionsInfoDocument = gql`
    query developerVersionsInfo($service: String, $version: DeveloperVersionInput) {
  developerVersionsInfo(service: $service, version: $version) {
    service
    version {
      distribution
      build
    }
    buildInfo {
      author
      sources {
        name
        git {
          url
          branch
          cloneSubmodules
        }
      }
      time
      comment
    }
  }
}
    `;

/**
 * __useDeveloperVersionsInfoQuery__
 *
 * To run a query within a React component, call `useDeveloperVersionsInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useDeveloperVersionsInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useDeveloperVersionsInfoQuery({
 *   variables: {
 *      service: // value for 'service'
 *      version: // value for 'version'
 *   },
 * });
 */
export function useDeveloperVersionsInfoQuery(baseOptions?: Apollo.QueryHookOptions<DeveloperVersionsInfoQuery, DeveloperVersionsInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<DeveloperVersionsInfoQuery, DeveloperVersionsInfoQueryVariables>(DeveloperVersionsInfoDocument, options);
      }
export function useDeveloperVersionsInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<DeveloperVersionsInfoQuery, DeveloperVersionsInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<DeveloperVersionsInfoQuery, DeveloperVersionsInfoQueryVariables>(DeveloperVersionsInfoDocument, options);
        }
export type DeveloperVersionsInfoQueryHookResult = ReturnType<typeof useDeveloperVersionsInfoQuery>;
export type DeveloperVersionsInfoLazyQueryHookResult = ReturnType<typeof useDeveloperVersionsInfoLazyQuery>;
export type DeveloperVersionsInfoQueryResult = Apollo.QueryResult<DeveloperVersionsInfoQuery, DeveloperVersionsInfoQueryVariables>;
export const DeveloperDesiredVersionsDocument = gql`
    query developerDesiredVersions {
  developerDesiredVersions {
    service
    version {
      distribution
      build
    }
  }
}
    `;

/**
 * __useDeveloperDesiredVersionsQuery__
 *
 * To run a query within a React component, call `useDeveloperDesiredVersionsQuery` and pass it any options that fit your needs.
 * When your component renders, `useDeveloperDesiredVersionsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useDeveloperDesiredVersionsQuery({
 *   variables: {
 *   },
 * });
 */
export function useDeveloperDesiredVersionsQuery(baseOptions?: Apollo.QueryHookOptions<DeveloperDesiredVersionsQuery, DeveloperDesiredVersionsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<DeveloperDesiredVersionsQuery, DeveloperDesiredVersionsQueryVariables>(DeveloperDesiredVersionsDocument, options);
      }
export function useDeveloperDesiredVersionsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<DeveloperDesiredVersionsQuery, DeveloperDesiredVersionsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<DeveloperDesiredVersionsQuery, DeveloperDesiredVersionsQueryVariables>(DeveloperDesiredVersionsDocument, options);
        }
export type DeveloperDesiredVersionsQueryHookResult = ReturnType<typeof useDeveloperDesiredVersionsQuery>;
export type DeveloperDesiredVersionsLazyQueryHookResult = ReturnType<typeof useDeveloperDesiredVersionsLazyQuery>;
export type DeveloperDesiredVersionsQueryResult = Apollo.QueryResult<DeveloperDesiredVersionsQuery, DeveloperDesiredVersionsQueryVariables>;
export const DeveloperDesiredVersionsHistoryDocument = gql`
    query developerDesiredVersionsHistory($limit: Int!) {
  developerDesiredVersionsHistory(limit: $limit) {
    time
    author
    versions {
      service
      version {
        distribution
        build
      }
    }
  }
}
    `;

/**
 * __useDeveloperDesiredVersionsHistoryQuery__
 *
 * To run a query within a React component, call `useDeveloperDesiredVersionsHistoryQuery` and pass it any options that fit your needs.
 * When your component renders, `useDeveloperDesiredVersionsHistoryQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useDeveloperDesiredVersionsHistoryQuery({
 *   variables: {
 *      limit: // value for 'limit'
 *   },
 * });
 */
export function useDeveloperDesiredVersionsHistoryQuery(baseOptions: Apollo.QueryHookOptions<DeveloperDesiredVersionsHistoryQuery, DeveloperDesiredVersionsHistoryQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<DeveloperDesiredVersionsHistoryQuery, DeveloperDesiredVersionsHistoryQueryVariables>(DeveloperDesiredVersionsHistoryDocument, options);
      }
export function useDeveloperDesiredVersionsHistoryLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<DeveloperDesiredVersionsHistoryQuery, DeveloperDesiredVersionsHistoryQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<DeveloperDesiredVersionsHistoryQuery, DeveloperDesiredVersionsHistoryQueryVariables>(DeveloperDesiredVersionsHistoryDocument, options);
        }
export type DeveloperDesiredVersionsHistoryQueryHookResult = ReturnType<typeof useDeveloperDesiredVersionsHistoryQuery>;
export type DeveloperDesiredVersionsHistoryLazyQueryHookResult = ReturnType<typeof useDeveloperDesiredVersionsHistoryLazyQuery>;
export type DeveloperDesiredVersionsHistoryQueryResult = Apollo.QueryResult<DeveloperDesiredVersionsHistoryQuery, DeveloperDesiredVersionsHistoryQueryVariables>;
export const ProviderDesiredVersionsDocument = gql`
    query providerDesiredVersions($distribution: String!) {
  providerDesiredVersions(distribution: $distribution) {
    service
    version {
      distribution
      build
    }
  }
}
    `;

/**
 * __useProviderDesiredVersionsQuery__
 *
 * To run a query within a React component, call `useProviderDesiredVersionsQuery` and pass it any options that fit your needs.
 * When your component renders, `useProviderDesiredVersionsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useProviderDesiredVersionsQuery({
 *   variables: {
 *      distribution: // value for 'distribution'
 *   },
 * });
 */
export function useProviderDesiredVersionsQuery(baseOptions: Apollo.QueryHookOptions<ProviderDesiredVersionsQuery, ProviderDesiredVersionsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ProviderDesiredVersionsQuery, ProviderDesiredVersionsQueryVariables>(ProviderDesiredVersionsDocument, options);
      }
export function useProviderDesiredVersionsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ProviderDesiredVersionsQuery, ProviderDesiredVersionsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ProviderDesiredVersionsQuery, ProviderDesiredVersionsQueryVariables>(ProviderDesiredVersionsDocument, options);
        }
export type ProviderDesiredVersionsQueryHookResult = ReturnType<typeof useProviderDesiredVersionsQuery>;
export type ProviderDesiredVersionsLazyQueryHookResult = ReturnType<typeof useProviderDesiredVersionsLazyQuery>;
export type ProviderDesiredVersionsQueryResult = Apollo.QueryResult<ProviderDesiredVersionsQuery, ProviderDesiredVersionsQueryVariables>;
export const BuildDeveloperVersionDocument = gql`
    mutation buildDeveloperVersion($service: String!, $version: DeveloperVersionInput!, $comment: String!, $buildClientVersion: Boolean!) {
  buildDeveloperVersion(
    service: $service
    version: $version
    comment: $comment
    buildClientVersion: $buildClientVersion
  )
}
    `;
export type BuildDeveloperVersionMutationFn = Apollo.MutationFunction<BuildDeveloperVersionMutation, BuildDeveloperVersionMutationVariables>;

/**
 * __useBuildDeveloperVersionMutation__
 *
 * To run a mutation, you first call `useBuildDeveloperVersionMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useBuildDeveloperVersionMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [buildDeveloperVersionMutation, { data, loading, error }] = useBuildDeveloperVersionMutation({
 *   variables: {
 *      service: // value for 'service'
 *      version: // value for 'version'
 *      comment: // value for 'comment'
 *      buildClientVersion: // value for 'buildClientVersion'
 *   },
 * });
 */
export function useBuildDeveloperVersionMutation(baseOptions?: Apollo.MutationHookOptions<BuildDeveloperVersionMutation, BuildDeveloperVersionMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<BuildDeveloperVersionMutation, BuildDeveloperVersionMutationVariables>(BuildDeveloperVersionDocument, options);
      }
export type BuildDeveloperVersionMutationHookResult = ReturnType<typeof useBuildDeveloperVersionMutation>;
export type BuildDeveloperVersionMutationResult = Apollo.MutationResult<BuildDeveloperVersionMutation>;
export type BuildDeveloperVersionMutationOptions = Apollo.BaseMutationOptions<BuildDeveloperVersionMutation, BuildDeveloperVersionMutationVariables>;
export const AddDeveloperVersionInfoDocument = gql`
    mutation addDeveloperVersionInfo($info: DeveloperVersionInfoInput!) {
  addDeveloperVersionInfo(info: $info)
}
    `;
export type AddDeveloperVersionInfoMutationFn = Apollo.MutationFunction<AddDeveloperVersionInfoMutation, AddDeveloperVersionInfoMutationVariables>;

/**
 * __useAddDeveloperVersionInfoMutation__
 *
 * To run a mutation, you first call `useAddDeveloperVersionInfoMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useAddDeveloperVersionInfoMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [addDeveloperVersionInfoMutation, { data, loading, error }] = useAddDeveloperVersionInfoMutation({
 *   variables: {
 *      info: // value for 'info'
 *   },
 * });
 */
export function useAddDeveloperVersionInfoMutation(baseOptions?: Apollo.MutationHookOptions<AddDeveloperVersionInfoMutation, AddDeveloperVersionInfoMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<AddDeveloperVersionInfoMutation, AddDeveloperVersionInfoMutationVariables>(AddDeveloperVersionInfoDocument, options);
      }
export type AddDeveloperVersionInfoMutationHookResult = ReturnType<typeof useAddDeveloperVersionInfoMutation>;
export type AddDeveloperVersionInfoMutationResult = Apollo.MutationResult<AddDeveloperVersionInfoMutation>;
export type AddDeveloperVersionInfoMutationOptions = Apollo.BaseMutationOptions<AddDeveloperVersionInfoMutation, AddDeveloperVersionInfoMutationVariables>;
export const RemoveDeveloperVersionDocument = gql`
    mutation removeDeveloperVersion($service: String!, $version: DeveloperDistributionVersionInput!) {
  removeDeveloperVersion(service: $service, version: $version)
}
    `;
export type RemoveDeveloperVersionMutationFn = Apollo.MutationFunction<RemoveDeveloperVersionMutation, RemoveDeveloperVersionMutationVariables>;

/**
 * __useRemoveDeveloperVersionMutation__
 *
 * To run a mutation, you first call `useRemoveDeveloperVersionMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useRemoveDeveloperVersionMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [removeDeveloperVersionMutation, { data, loading, error }] = useRemoveDeveloperVersionMutation({
 *   variables: {
 *      service: // value for 'service'
 *      version: // value for 'version'
 *   },
 * });
 */
export function useRemoveDeveloperVersionMutation(baseOptions?: Apollo.MutationHookOptions<RemoveDeveloperVersionMutation, RemoveDeveloperVersionMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<RemoveDeveloperVersionMutation, RemoveDeveloperVersionMutationVariables>(RemoveDeveloperVersionDocument, options);
      }
export type RemoveDeveloperVersionMutationHookResult = ReturnType<typeof useRemoveDeveloperVersionMutation>;
export type RemoveDeveloperVersionMutationResult = Apollo.MutationResult<RemoveDeveloperVersionMutation>;
export type RemoveDeveloperVersionMutationOptions = Apollo.BaseMutationOptions<RemoveDeveloperVersionMutation, RemoveDeveloperVersionMutationVariables>;
export const SetDeveloperDesiredVersionsDocument = gql`
    mutation setDeveloperDesiredVersions($versions: [DeveloperDesiredVersionDeltaInput!]!) {
  setDeveloperDesiredVersions(versions: $versions)
}
    `;
export type SetDeveloperDesiredVersionsMutationFn = Apollo.MutationFunction<SetDeveloperDesiredVersionsMutation, SetDeveloperDesiredVersionsMutationVariables>;

/**
 * __useSetDeveloperDesiredVersionsMutation__
 *
 * To run a mutation, you first call `useSetDeveloperDesiredVersionsMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useSetDeveloperDesiredVersionsMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [setDeveloperDesiredVersionsMutation, { data, loading, error }] = useSetDeveloperDesiredVersionsMutation({
 *   variables: {
 *      versions: // value for 'versions'
 *   },
 * });
 */
export function useSetDeveloperDesiredVersionsMutation(baseOptions?: Apollo.MutationHookOptions<SetDeveloperDesiredVersionsMutation, SetDeveloperDesiredVersionsMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<SetDeveloperDesiredVersionsMutation, SetDeveloperDesiredVersionsMutationVariables>(SetDeveloperDesiredVersionsDocument, options);
      }
export type SetDeveloperDesiredVersionsMutationHookResult = ReturnType<typeof useSetDeveloperDesiredVersionsMutation>;
export type SetDeveloperDesiredVersionsMutationResult = Apollo.MutationResult<SetDeveloperDesiredVersionsMutation>;
export type SetDeveloperDesiredVersionsMutationOptions = Apollo.BaseMutationOptions<SetDeveloperDesiredVersionsMutation, SetDeveloperDesiredVersionsMutationVariables>;
export const SetTestedVersionsDocument = gql`
    mutation setTestedVersions($versions: [DeveloperDesiredVersionInput!]!) {
  setTestedVersions(versions: $versions)
}
    `;
export type SetTestedVersionsMutationFn = Apollo.MutationFunction<SetTestedVersionsMutation, SetTestedVersionsMutationVariables>;

/**
 * __useSetTestedVersionsMutation__
 *
 * To run a mutation, you first call `useSetTestedVersionsMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useSetTestedVersionsMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [setTestedVersionsMutation, { data, loading, error }] = useSetTestedVersionsMutation({
 *   variables: {
 *      versions: // value for 'versions'
 *   },
 * });
 */
export function useSetTestedVersionsMutation(baseOptions?: Apollo.MutationHookOptions<SetTestedVersionsMutation, SetTestedVersionsMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<SetTestedVersionsMutation, SetTestedVersionsMutationVariables>(SetTestedVersionsDocument, options);
      }
export type SetTestedVersionsMutationHookResult = ReturnType<typeof useSetTestedVersionsMutation>;
export type SetTestedVersionsMutationResult = Apollo.MutationResult<SetTestedVersionsMutation>;
export type SetTestedVersionsMutationOptions = Apollo.BaseMutationOptions<SetTestedVersionsMutation, SetTestedVersionsMutationVariables>;
export const TestedVersionsDocument = gql`
    query testedVersions {
  testedVersions {
    service
    version {
      distribution
      build
    }
  }
}
    `;

/**
 * __useTestedVersionsQuery__
 *
 * To run a query within a React component, call `useTestedVersionsQuery` and pass it any options that fit your needs.
 * When your component renders, `useTestedVersionsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useTestedVersionsQuery({
 *   variables: {
 *   },
 * });
 */
export function useTestedVersionsQuery(baseOptions?: Apollo.QueryHookOptions<TestedVersionsQuery, TestedVersionsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<TestedVersionsQuery, TestedVersionsQueryVariables>(TestedVersionsDocument, options);
      }
export function useTestedVersionsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<TestedVersionsQuery, TestedVersionsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<TestedVersionsQuery, TestedVersionsQueryVariables>(TestedVersionsDocument, options);
        }
export type TestedVersionsQueryHookResult = ReturnType<typeof useTestedVersionsQuery>;
export type TestedVersionsLazyQueryHookResult = ReturnType<typeof useTestedVersionsLazyQuery>;
export type TestedVersionsQueryResult = Apollo.QueryResult<TestedVersionsQuery, TestedVersionsQueryVariables>;
export const SetProviderTestedVersionsDocument = gql`
    mutation setProviderTestedVersions($distribution: String!, $versions: [DeveloperDesiredVersionInput!]!) {
  setProviderTestedVersions(distribution: $distribution, versions: $versions)
}
    `;
export type SetProviderTestedVersionsMutationFn = Apollo.MutationFunction<SetProviderTestedVersionsMutation, SetProviderTestedVersionsMutationVariables>;

/**
 * __useSetProviderTestedVersionsMutation__
 *
 * To run a mutation, you first call `useSetProviderTestedVersionsMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useSetProviderTestedVersionsMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [setProviderTestedVersionsMutation, { data, loading, error }] = useSetProviderTestedVersionsMutation({
 *   variables: {
 *      distribution: // value for 'distribution'
 *      versions: // value for 'versions'
 *   },
 * });
 */
export function useSetProviderTestedVersionsMutation(baseOptions?: Apollo.MutationHookOptions<SetProviderTestedVersionsMutation, SetProviderTestedVersionsMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<SetProviderTestedVersionsMutation, SetProviderTestedVersionsMutationVariables>(SetProviderTestedVersionsDocument, options);
      }
export type SetProviderTestedVersionsMutationHookResult = ReturnType<typeof useSetProviderTestedVersionsMutation>;
export type SetProviderTestedVersionsMutationResult = Apollo.MutationResult<SetProviderTestedVersionsMutation>;
export type SetProviderTestedVersionsMutationOptions = Apollo.BaseMutationOptions<SetProviderTestedVersionsMutation, SetProviderTestedVersionsMutationVariables>;
export const ProviderTestedVersionsDocument = gql`
    query providerTestedVersions($distribution: String!) {
  providerTestedVersions(distribution: $distribution) {
    service
    version {
      distribution
      build
    }
  }
}
    `;

/**
 * __useProviderTestedVersionsQuery__
 *
 * To run a query within a React component, call `useProviderTestedVersionsQuery` and pass it any options that fit your needs.
 * When your component renders, `useProviderTestedVersionsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useProviderTestedVersionsQuery({
 *   variables: {
 *      distribution: // value for 'distribution'
 *   },
 * });
 */
export function useProviderTestedVersionsQuery(baseOptions: Apollo.QueryHookOptions<ProviderTestedVersionsQuery, ProviderTestedVersionsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ProviderTestedVersionsQuery, ProviderTestedVersionsQueryVariables>(ProviderTestedVersionsDocument, options);
      }
export function useProviderTestedVersionsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ProviderTestedVersionsQuery, ProviderTestedVersionsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ProviderTestedVersionsQuery, ProviderTestedVersionsQueryVariables>(ProviderTestedVersionsDocument, options);
        }
export type ProviderTestedVersionsQueryHookResult = ReturnType<typeof useProviderTestedVersionsQuery>;
export type ProviderTestedVersionsLazyQueryHookResult = ReturnType<typeof useProviderTestedVersionsLazyQuery>;
export type ProviderTestedVersionsQueryResult = Apollo.QueryResult<ProviderTestedVersionsQuery, ProviderTestedVersionsQueryVariables>;
export const ClientVersionsInfoDocument = gql`
    query clientVersionsInfo($service: String, $version: ClientVersionInput) {
  clientVersionsInfo(service: $service, version: $version) {
    service
    version {
      distribution
      developerBuild
      clientBuild
    }
    buildInfo {
      author
      sources {
        name
        git {
          url
          branch
          cloneSubmodules
        }
      }
      time
      comment
    }
    installInfo {
      account
      time
    }
  }
}
    `;

/**
 * __useClientVersionsInfoQuery__
 *
 * To run a query within a React component, call `useClientVersionsInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useClientVersionsInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useClientVersionsInfoQuery({
 *   variables: {
 *      service: // value for 'service'
 *      version: // value for 'version'
 *   },
 * });
 */
export function useClientVersionsInfoQuery(baseOptions?: Apollo.QueryHookOptions<ClientVersionsInfoQuery, ClientVersionsInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ClientVersionsInfoQuery, ClientVersionsInfoQueryVariables>(ClientVersionsInfoDocument, options);
      }
export function useClientVersionsInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ClientVersionsInfoQuery, ClientVersionsInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ClientVersionsInfoQuery, ClientVersionsInfoQueryVariables>(ClientVersionsInfoDocument, options);
        }
export type ClientVersionsInfoQueryHookResult = ReturnType<typeof useClientVersionsInfoQuery>;
export type ClientVersionsInfoLazyQueryHookResult = ReturnType<typeof useClientVersionsInfoLazyQuery>;
export type ClientVersionsInfoQueryResult = Apollo.QueryResult<ClientVersionsInfoQuery, ClientVersionsInfoQueryVariables>;
export const ClientDesiredVersionsDocument = gql`
    query clientDesiredVersions {
  clientDesiredVersions {
    service
    version {
      distribution
      developerBuild
      clientBuild
    }
  }
}
    `;

/**
 * __useClientDesiredVersionsQuery__
 *
 * To run a query within a React component, call `useClientDesiredVersionsQuery` and pass it any options that fit your needs.
 * When your component renders, `useClientDesiredVersionsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useClientDesiredVersionsQuery({
 *   variables: {
 *   },
 * });
 */
export function useClientDesiredVersionsQuery(baseOptions?: Apollo.QueryHookOptions<ClientDesiredVersionsQuery, ClientDesiredVersionsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ClientDesiredVersionsQuery, ClientDesiredVersionsQueryVariables>(ClientDesiredVersionsDocument, options);
      }
export function useClientDesiredVersionsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ClientDesiredVersionsQuery, ClientDesiredVersionsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ClientDesiredVersionsQuery, ClientDesiredVersionsQueryVariables>(ClientDesiredVersionsDocument, options);
        }
export type ClientDesiredVersionsQueryHookResult = ReturnType<typeof useClientDesiredVersionsQuery>;
export type ClientDesiredVersionsLazyQueryHookResult = ReturnType<typeof useClientDesiredVersionsLazyQuery>;
export type ClientDesiredVersionsQueryResult = Apollo.QueryResult<ClientDesiredVersionsQuery, ClientDesiredVersionsQueryVariables>;
export const ClientDesiredVersionsHistoryDocument = gql`
    query clientDesiredVersionsHistory($limit: Int!) {
  clientDesiredVersionsHistory(limit: $limit) {
    time
    author
    versions {
      service
      version {
        distribution
        developerBuild
        clientBuild
      }
    }
  }
}
    `;

/**
 * __useClientDesiredVersionsHistoryQuery__
 *
 * To run a query within a React component, call `useClientDesiredVersionsHistoryQuery` and pass it any options that fit your needs.
 * When your component renders, `useClientDesiredVersionsHistoryQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useClientDesiredVersionsHistoryQuery({
 *   variables: {
 *      limit: // value for 'limit'
 *   },
 * });
 */
export function useClientDesiredVersionsHistoryQuery(baseOptions: Apollo.QueryHookOptions<ClientDesiredVersionsHistoryQuery, ClientDesiredVersionsHistoryQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ClientDesiredVersionsHistoryQuery, ClientDesiredVersionsHistoryQueryVariables>(ClientDesiredVersionsHistoryDocument, options);
      }
export function useClientDesiredVersionsHistoryLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ClientDesiredVersionsHistoryQuery, ClientDesiredVersionsHistoryQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ClientDesiredVersionsHistoryQuery, ClientDesiredVersionsHistoryQueryVariables>(ClientDesiredVersionsHistoryDocument, options);
        }
export type ClientDesiredVersionsHistoryQueryHookResult = ReturnType<typeof useClientDesiredVersionsHistoryQuery>;
export type ClientDesiredVersionsHistoryLazyQueryHookResult = ReturnType<typeof useClientDesiredVersionsHistoryLazyQuery>;
export type ClientDesiredVersionsHistoryQueryResult = Apollo.QueryResult<ClientDesiredVersionsHistoryQuery, ClientDesiredVersionsHistoryQueryVariables>;
export const InstalledDesiredVersionsDocument = gql`
    query installedDesiredVersions($distribution: String!) {
  installedDesiredVersions(distribution: $distribution) {
    service
    version {
      distribution
      developerBuild
      clientBuild
    }
  }
}
    `;

/**
 * __useInstalledDesiredVersionsQuery__
 *
 * To run a query within a React component, call `useInstalledDesiredVersionsQuery` and pass it any options that fit your needs.
 * When your component renders, `useInstalledDesiredVersionsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useInstalledDesiredVersionsQuery({
 *   variables: {
 *      distribution: // value for 'distribution'
 *   },
 * });
 */
export function useInstalledDesiredVersionsQuery(baseOptions: Apollo.QueryHookOptions<InstalledDesiredVersionsQuery, InstalledDesiredVersionsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<InstalledDesiredVersionsQuery, InstalledDesiredVersionsQueryVariables>(InstalledDesiredVersionsDocument, options);
      }
export function useInstalledDesiredVersionsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<InstalledDesiredVersionsQuery, InstalledDesiredVersionsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<InstalledDesiredVersionsQuery, InstalledDesiredVersionsQueryVariables>(InstalledDesiredVersionsDocument, options);
        }
export type InstalledDesiredVersionsQueryHookResult = ReturnType<typeof useInstalledDesiredVersionsQuery>;
export type InstalledDesiredVersionsLazyQueryHookResult = ReturnType<typeof useInstalledDesiredVersionsLazyQuery>;
export type InstalledDesiredVersionsQueryResult = Apollo.QueryResult<InstalledDesiredVersionsQuery, InstalledDesiredVersionsQueryVariables>;
export const BuildClientVersionsDocument = gql`
    mutation buildClientVersions($versions: [DeveloperDesiredVersionInput!]!) {
  buildClientVersions(versions: $versions)
}
    `;
export type BuildClientVersionsMutationFn = Apollo.MutationFunction<BuildClientVersionsMutation, BuildClientVersionsMutationVariables>;

/**
 * __useBuildClientVersionsMutation__
 *
 * To run a mutation, you first call `useBuildClientVersionsMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useBuildClientVersionsMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [buildClientVersionsMutation, { data, loading, error }] = useBuildClientVersionsMutation({
 *   variables: {
 *      versions: // value for 'versions'
 *   },
 * });
 */
export function useBuildClientVersionsMutation(baseOptions?: Apollo.MutationHookOptions<BuildClientVersionsMutation, BuildClientVersionsMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<BuildClientVersionsMutation, BuildClientVersionsMutationVariables>(BuildClientVersionsDocument, options);
      }
export type BuildClientVersionsMutationHookResult = ReturnType<typeof useBuildClientVersionsMutation>;
export type BuildClientVersionsMutationResult = Apollo.MutationResult<BuildClientVersionsMutation>;
export type BuildClientVersionsMutationOptions = Apollo.BaseMutationOptions<BuildClientVersionsMutation, BuildClientVersionsMutationVariables>;
export const AddClientVersionInfoDocument = gql`
    mutation addClientVersionInfo($info: ClientVersionInfoInput!) {
  addClientVersionInfo(info: $info)
}
    `;
export type AddClientVersionInfoMutationFn = Apollo.MutationFunction<AddClientVersionInfoMutation, AddClientVersionInfoMutationVariables>;

/**
 * __useAddClientVersionInfoMutation__
 *
 * To run a mutation, you first call `useAddClientVersionInfoMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useAddClientVersionInfoMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [addClientVersionInfoMutation, { data, loading, error }] = useAddClientVersionInfoMutation({
 *   variables: {
 *      info: // value for 'info'
 *   },
 * });
 */
export function useAddClientVersionInfoMutation(baseOptions?: Apollo.MutationHookOptions<AddClientVersionInfoMutation, AddClientVersionInfoMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<AddClientVersionInfoMutation, AddClientVersionInfoMutationVariables>(AddClientVersionInfoDocument, options);
      }
export type AddClientVersionInfoMutationHookResult = ReturnType<typeof useAddClientVersionInfoMutation>;
export type AddClientVersionInfoMutationResult = Apollo.MutationResult<AddClientVersionInfoMutation>;
export type AddClientVersionInfoMutationOptions = Apollo.BaseMutationOptions<AddClientVersionInfoMutation, AddClientVersionInfoMutationVariables>;
export const RemoveClientVersionDocument = gql`
    mutation removeClientVersion($service: String!, $version: ClientDistributionVersionInput!) {
  removeClientVersion(service: $service, version: $version)
}
    `;
export type RemoveClientVersionMutationFn = Apollo.MutationFunction<RemoveClientVersionMutation, RemoveClientVersionMutationVariables>;

/**
 * __useRemoveClientVersionMutation__
 *
 * To run a mutation, you first call `useRemoveClientVersionMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useRemoveClientVersionMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [removeClientVersionMutation, { data, loading, error }] = useRemoveClientVersionMutation({
 *   variables: {
 *      service: // value for 'service'
 *      version: // value for 'version'
 *   },
 * });
 */
export function useRemoveClientVersionMutation(baseOptions?: Apollo.MutationHookOptions<RemoveClientVersionMutation, RemoveClientVersionMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<RemoveClientVersionMutation, RemoveClientVersionMutationVariables>(RemoveClientVersionDocument, options);
      }
export type RemoveClientVersionMutationHookResult = ReturnType<typeof useRemoveClientVersionMutation>;
export type RemoveClientVersionMutationResult = Apollo.MutationResult<RemoveClientVersionMutation>;
export type RemoveClientVersionMutationOptions = Apollo.BaseMutationOptions<RemoveClientVersionMutation, RemoveClientVersionMutationVariables>;
export const SetClientDesiredVersionsDocument = gql`
    mutation setClientDesiredVersions($versions: [ClientDesiredVersionDeltaInput!]!) {
  setClientDesiredVersions(versions: $versions)
}
    `;
export type SetClientDesiredVersionsMutationFn = Apollo.MutationFunction<SetClientDesiredVersionsMutation, SetClientDesiredVersionsMutationVariables>;

/**
 * __useSetClientDesiredVersionsMutation__
 *
 * To run a mutation, you first call `useSetClientDesiredVersionsMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useSetClientDesiredVersionsMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [setClientDesiredVersionsMutation, { data, loading, error }] = useSetClientDesiredVersionsMutation({
 *   variables: {
 *      versions: // value for 'versions'
 *   },
 * });
 */
export function useSetClientDesiredVersionsMutation(baseOptions?: Apollo.MutationHookOptions<SetClientDesiredVersionsMutation, SetClientDesiredVersionsMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<SetClientDesiredVersionsMutation, SetClientDesiredVersionsMutationVariables>(SetClientDesiredVersionsDocument, options);
      }
export type SetClientDesiredVersionsMutationHookResult = ReturnType<typeof useSetClientDesiredVersionsMutation>;
export type SetClientDesiredVersionsMutationResult = Apollo.MutationResult<SetClientDesiredVersionsMutation>;
export type SetClientDesiredVersionsMutationOptions = Apollo.BaseMutationOptions<SetClientDesiredVersionsMutation, SetClientDesiredVersionsMutationVariables>;
export const ServiceStatesDocument = gql`
    query serviceStates($distribution: String!) {
  serviceStates(distribution: $distribution) {
    distribution
    payload {
      instance
      service
      directory
      state {
        time
        installTime
        startTime
        version {
          distribution
          developerBuild
          clientBuild
        }
        updateToVersion {
          distribution
          developerBuild
          clientBuild
        }
        updateError {
          critical
          error
        }
        failuresCount
        lastExitCode
      }
    }
  }
}
    `;

/**
 * __useServiceStatesQuery__
 *
 * To run a query within a React component, call `useServiceStatesQuery` and pass it any options that fit your needs.
 * When your component renders, `useServiceStatesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useServiceStatesQuery({
 *   variables: {
 *      distribution: // value for 'distribution'
 *   },
 * });
 */
export function useServiceStatesQuery(baseOptions: Apollo.QueryHookOptions<ServiceStatesQuery, ServiceStatesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ServiceStatesQuery, ServiceStatesQueryVariables>(ServiceStatesDocument, options);
      }
export function useServiceStatesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ServiceStatesQuery, ServiceStatesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ServiceStatesQuery, ServiceStatesQueryVariables>(ServiceStatesDocument, options);
        }
export type ServiceStatesQueryHookResult = ReturnType<typeof useServiceStatesQuery>;
export type ServiceStatesLazyQueryHookResult = ReturnType<typeof useServiceStatesLazyQuery>;
export type ServiceStatesQueryResult = Apollo.QueryResult<ServiceStatesQuery, ServiceStatesQueryVariables>;
export const LogServicesDocument = gql`
    query logServices {
  logServices
}
    `;

/**
 * __useLogServicesQuery__
 *
 * To run a query within a React component, call `useLogServicesQuery` and pass it any options that fit your needs.
 * When your component renders, `useLogServicesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useLogServicesQuery({
 *   variables: {
 *   },
 * });
 */
export function useLogServicesQuery(baseOptions?: Apollo.QueryHookOptions<LogServicesQuery, LogServicesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<LogServicesQuery, LogServicesQueryVariables>(LogServicesDocument, options);
      }
export function useLogServicesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<LogServicesQuery, LogServicesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<LogServicesQuery, LogServicesQueryVariables>(LogServicesDocument, options);
        }
export type LogServicesQueryHookResult = ReturnType<typeof useLogServicesQuery>;
export type LogServicesLazyQueryHookResult = ReturnType<typeof useLogServicesLazyQuery>;
export type LogServicesQueryResult = Apollo.QueryResult<LogServicesQuery, LogServicesQueryVariables>;
export const LogInstancesDocument = gql`
    query logInstances($service: String!) {
  logInstances(service: $service)
}
    `;

/**
 * __useLogInstancesQuery__
 *
 * To run a query within a React component, call `useLogInstancesQuery` and pass it any options that fit your needs.
 * When your component renders, `useLogInstancesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useLogInstancesQuery({
 *   variables: {
 *      service: // value for 'service'
 *   },
 * });
 */
export function useLogInstancesQuery(baseOptions: Apollo.QueryHookOptions<LogInstancesQuery, LogInstancesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<LogInstancesQuery, LogInstancesQueryVariables>(LogInstancesDocument, options);
      }
export function useLogInstancesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<LogInstancesQuery, LogInstancesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<LogInstancesQuery, LogInstancesQueryVariables>(LogInstancesDocument, options);
        }
export type LogInstancesQueryHookResult = ReturnType<typeof useLogInstancesQuery>;
export type LogInstancesLazyQueryHookResult = ReturnType<typeof useLogInstancesLazyQuery>;
export type LogInstancesQueryResult = Apollo.QueryResult<LogInstancesQuery, LogInstancesQueryVariables>;
export const LogDirectoriesDocument = gql`
    query logDirectories($service: String!, $instance: String!) {
  logDirectories(service: $service, instance: $instance)
}
    `;

/**
 * __useLogDirectoriesQuery__
 *
 * To run a query within a React component, call `useLogDirectoriesQuery` and pass it any options that fit your needs.
 * When your component renders, `useLogDirectoriesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useLogDirectoriesQuery({
 *   variables: {
 *      service: // value for 'service'
 *      instance: // value for 'instance'
 *   },
 * });
 */
export function useLogDirectoriesQuery(baseOptions: Apollo.QueryHookOptions<LogDirectoriesQuery, LogDirectoriesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<LogDirectoriesQuery, LogDirectoriesQueryVariables>(LogDirectoriesDocument, options);
      }
export function useLogDirectoriesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<LogDirectoriesQuery, LogDirectoriesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<LogDirectoriesQuery, LogDirectoriesQueryVariables>(LogDirectoriesDocument, options);
        }
export type LogDirectoriesQueryHookResult = ReturnType<typeof useLogDirectoriesQuery>;
export type LogDirectoriesLazyQueryHookResult = ReturnType<typeof useLogDirectoriesLazyQuery>;
export type LogDirectoriesQueryResult = Apollo.QueryResult<LogDirectoriesQuery, LogDirectoriesQueryVariables>;
export const LogProcessesDocument = gql`
    query logProcesses($service: String!, $instance: String!, $directory: String!) {
  logProcesses(service: $service, instance: $instance, directory: $directory)
}
    `;

/**
 * __useLogProcessesQuery__
 *
 * To run a query within a React component, call `useLogProcessesQuery` and pass it any options that fit your needs.
 * When your component renders, `useLogProcessesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useLogProcessesQuery({
 *   variables: {
 *      service: // value for 'service'
 *      instance: // value for 'instance'
 *      directory: // value for 'directory'
 *   },
 * });
 */
export function useLogProcessesQuery(baseOptions: Apollo.QueryHookOptions<LogProcessesQuery, LogProcessesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<LogProcessesQuery, LogProcessesQueryVariables>(LogProcessesDocument, options);
      }
export function useLogProcessesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<LogProcessesQuery, LogProcessesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<LogProcessesQuery, LogProcessesQueryVariables>(LogProcessesDocument, options);
        }
export type LogProcessesQueryHookResult = ReturnType<typeof useLogProcessesQuery>;
export type LogProcessesLazyQueryHookResult = ReturnType<typeof useLogProcessesLazyQuery>;
export type LogProcessesQueryResult = Apollo.QueryResult<LogProcessesQuery, LogProcessesQueryVariables>;
export const LogLevelsDocument = gql`
    query logLevels($service: String, $instance: String, $directory: String, $process: String, $task: String) {
  logLevels(
    service: $service
    instance: $instance
    directory: $directory
    process: $process
    task: $task
  )
}
    `;

/**
 * __useLogLevelsQuery__
 *
 * To run a query within a React component, call `useLogLevelsQuery` and pass it any options that fit your needs.
 * When your component renders, `useLogLevelsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useLogLevelsQuery({
 *   variables: {
 *      service: // value for 'service'
 *      instance: // value for 'instance'
 *      directory: // value for 'directory'
 *      process: // value for 'process'
 *      task: // value for 'task'
 *   },
 * });
 */
export function useLogLevelsQuery(baseOptions?: Apollo.QueryHookOptions<LogLevelsQuery, LogLevelsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<LogLevelsQuery, LogLevelsQueryVariables>(LogLevelsDocument, options);
      }
export function useLogLevelsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<LogLevelsQuery, LogLevelsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<LogLevelsQuery, LogLevelsQueryVariables>(LogLevelsDocument, options);
        }
export type LogLevelsQueryHookResult = ReturnType<typeof useLogLevelsQuery>;
export type LogLevelsLazyQueryHookResult = ReturnType<typeof useLogLevelsLazyQuery>;
export type LogLevelsQueryResult = Apollo.QueryResult<LogLevelsQuery, LogLevelsQueryVariables>;
export const LogsStartTimeDocument = gql`
    query logsStartTime($service: String, $instance: String, $directory: String, $process: String, $task: String) {
  logsStartTime(
    service: $service
    instance: $instance
    directory: $directory
    process: $process
    task: $task
  )
}
    `;

/**
 * __useLogsStartTimeQuery__
 *
 * To run a query within a React component, call `useLogsStartTimeQuery` and pass it any options that fit your needs.
 * When your component renders, `useLogsStartTimeQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useLogsStartTimeQuery({
 *   variables: {
 *      service: // value for 'service'
 *      instance: // value for 'instance'
 *      directory: // value for 'directory'
 *      process: // value for 'process'
 *      task: // value for 'task'
 *   },
 * });
 */
export function useLogsStartTimeQuery(baseOptions?: Apollo.QueryHookOptions<LogsStartTimeQuery, LogsStartTimeQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<LogsStartTimeQuery, LogsStartTimeQueryVariables>(LogsStartTimeDocument, options);
      }
export function useLogsStartTimeLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<LogsStartTimeQuery, LogsStartTimeQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<LogsStartTimeQuery, LogsStartTimeQueryVariables>(LogsStartTimeDocument, options);
        }
export type LogsStartTimeQueryHookResult = ReturnType<typeof useLogsStartTimeQuery>;
export type LogsStartTimeLazyQueryHookResult = ReturnType<typeof useLogsStartTimeLazyQuery>;
export type LogsStartTimeQueryResult = Apollo.QueryResult<LogsStartTimeQuery, LogsStartTimeQueryVariables>;
export const LogsEndTimeDocument = gql`
    query logsEndTime($service: String, $instance: String, $directory: String, $process: String, $task: String) {
  logsEndTime(
    service: $service
    instance: $instance
    directory: $directory
    process: $process
    task: $task
  )
}
    `;

/**
 * __useLogsEndTimeQuery__
 *
 * To run a query within a React component, call `useLogsEndTimeQuery` and pass it any options that fit your needs.
 * When your component renders, `useLogsEndTimeQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useLogsEndTimeQuery({
 *   variables: {
 *      service: // value for 'service'
 *      instance: // value for 'instance'
 *      directory: // value for 'directory'
 *      process: // value for 'process'
 *      task: // value for 'task'
 *   },
 * });
 */
export function useLogsEndTimeQuery(baseOptions?: Apollo.QueryHookOptions<LogsEndTimeQuery, LogsEndTimeQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<LogsEndTimeQuery, LogsEndTimeQueryVariables>(LogsEndTimeDocument, options);
      }
export function useLogsEndTimeLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<LogsEndTimeQuery, LogsEndTimeQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<LogsEndTimeQuery, LogsEndTimeQueryVariables>(LogsEndTimeDocument, options);
        }
export type LogsEndTimeQueryHookResult = ReturnType<typeof useLogsEndTimeQuery>;
export type LogsEndTimeLazyQueryHookResult = ReturnType<typeof useLogsEndTimeLazyQuery>;
export type LogsEndTimeQueryResult = Apollo.QueryResult<LogsEndTimeQuery, LogsEndTimeQueryVariables>;
export const TaskLogsDocument = gql`
    query taskLogs($task: String!, $from: BigInt, $to: BigInt, $fromTime: Date, $toTime: Date, $levels: [String!], $find: String, $limit: Int) {
  logs(
    task: $task
    from: $from
    to: $to
    fromTime: $fromTime
    toTime: $toTime
    levels: $levels
    find: $find
    limit: $limit
  ) {
    sequence
    instance
    directory
    process
    payload {
      time
      level
      unit
      message
      terminationStatus
    }
  }
}
    `;

/**
 * __useTaskLogsQuery__
 *
 * To run a query within a React component, call `useTaskLogsQuery` and pass it any options that fit your needs.
 * When your component renders, `useTaskLogsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useTaskLogsQuery({
 *   variables: {
 *      task: // value for 'task'
 *      from: // value for 'from'
 *      to: // value for 'to'
 *      fromTime: // value for 'fromTime'
 *      toTime: // value for 'toTime'
 *      levels: // value for 'levels'
 *      find: // value for 'find'
 *      limit: // value for 'limit'
 *   },
 * });
 */
export function useTaskLogsQuery(baseOptions: Apollo.QueryHookOptions<TaskLogsQuery, TaskLogsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<TaskLogsQuery, TaskLogsQueryVariables>(TaskLogsDocument, options);
      }
export function useTaskLogsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<TaskLogsQuery, TaskLogsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<TaskLogsQuery, TaskLogsQueryVariables>(TaskLogsDocument, options);
        }
export type TaskLogsQueryHookResult = ReturnType<typeof useTaskLogsQuery>;
export type TaskLogsLazyQueryHookResult = ReturnType<typeof useTaskLogsLazyQuery>;
export type TaskLogsQueryResult = Apollo.QueryResult<TaskLogsQuery, TaskLogsQueryVariables>;
export const ServiceLogsDocument = gql`
    query serviceLogs($service: String!, $from: BigInt, $to: BigInt, $fromTime: Date, $toTime: Date, $levels: [String!], $find: String, $limit: Int) {
  logs(
    service: $service
    from: $from
    to: $to
    fromTime: $fromTime
    toTime: $toTime
    levels: $levels
    find: $find
    limit: $limit
  ) {
    sequence
    instance
    directory
    process
    payload {
      time
      level
      unit
      message
      terminationStatus
    }
  }
}
    `;

/**
 * __useServiceLogsQuery__
 *
 * To run a query within a React component, call `useServiceLogsQuery` and pass it any options that fit your needs.
 * When your component renders, `useServiceLogsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useServiceLogsQuery({
 *   variables: {
 *      service: // value for 'service'
 *      from: // value for 'from'
 *      to: // value for 'to'
 *      fromTime: // value for 'fromTime'
 *      toTime: // value for 'toTime'
 *      levels: // value for 'levels'
 *      find: // value for 'find'
 *      limit: // value for 'limit'
 *   },
 * });
 */
export function useServiceLogsQuery(baseOptions: Apollo.QueryHookOptions<ServiceLogsQuery, ServiceLogsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ServiceLogsQuery, ServiceLogsQueryVariables>(ServiceLogsDocument, options);
      }
export function useServiceLogsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ServiceLogsQuery, ServiceLogsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ServiceLogsQuery, ServiceLogsQueryVariables>(ServiceLogsDocument, options);
        }
export type ServiceLogsQueryHookResult = ReturnType<typeof useServiceLogsQuery>;
export type ServiceLogsLazyQueryHookResult = ReturnType<typeof useServiceLogsLazyQuery>;
export type ServiceLogsQueryResult = Apollo.QueryResult<ServiceLogsQuery, ServiceLogsQueryVariables>;
export const InstanceLogsDocument = gql`
    query instanceLogs($service: String!, $instance: String!, $from: BigInt, $to: BigInt, $fromTime: Date, $toTime: Date, $levels: [String!], $find: String, $limit: Int) {
  logs(
    service: $service
    instance: $instance
    from: $from
    to: $to
    fromTime: $fromTime
    toTime: $toTime
    levels: $levels
    find: $find
    limit: $limit
  ) {
    sequence
    directory
    process
    payload {
      time
      level
      unit
      message
      terminationStatus
    }
  }
}
    `;

/**
 * __useInstanceLogsQuery__
 *
 * To run a query within a React component, call `useInstanceLogsQuery` and pass it any options that fit your needs.
 * When your component renders, `useInstanceLogsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useInstanceLogsQuery({
 *   variables: {
 *      service: // value for 'service'
 *      instance: // value for 'instance'
 *      from: // value for 'from'
 *      to: // value for 'to'
 *      fromTime: // value for 'fromTime'
 *      toTime: // value for 'toTime'
 *      levels: // value for 'levels'
 *      find: // value for 'find'
 *      limit: // value for 'limit'
 *   },
 * });
 */
export function useInstanceLogsQuery(baseOptions: Apollo.QueryHookOptions<InstanceLogsQuery, InstanceLogsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<InstanceLogsQuery, InstanceLogsQueryVariables>(InstanceLogsDocument, options);
      }
export function useInstanceLogsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<InstanceLogsQuery, InstanceLogsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<InstanceLogsQuery, InstanceLogsQueryVariables>(InstanceLogsDocument, options);
        }
export type InstanceLogsQueryHookResult = ReturnType<typeof useInstanceLogsQuery>;
export type InstanceLogsLazyQueryHookResult = ReturnType<typeof useInstanceLogsLazyQuery>;
export type InstanceLogsQueryResult = Apollo.QueryResult<InstanceLogsQuery, InstanceLogsQueryVariables>;
export const DirectoryLogsDocument = gql`
    query directoryLogs($service: String!, $instance: String!, $directory: String!, $from: BigInt, $to: BigInt, $fromTime: Date, $toTime: Date, $levels: [String!], $find: String, $limit: Int) {
  logs(
    service: $service
    instance: $instance
    directory: $directory
    from: $from
    to: $to
    fromTime: $fromTime
    toTime: $toTime
    levels: $levels
    find: $find
    limit: $limit
  ) {
    sequence
    process
    payload {
      time
      level
      unit
      message
      terminationStatus
    }
  }
}
    `;

/**
 * __useDirectoryLogsQuery__
 *
 * To run a query within a React component, call `useDirectoryLogsQuery` and pass it any options that fit your needs.
 * When your component renders, `useDirectoryLogsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useDirectoryLogsQuery({
 *   variables: {
 *      service: // value for 'service'
 *      instance: // value for 'instance'
 *      directory: // value for 'directory'
 *      from: // value for 'from'
 *      to: // value for 'to'
 *      fromTime: // value for 'fromTime'
 *      toTime: // value for 'toTime'
 *      levels: // value for 'levels'
 *      find: // value for 'find'
 *      limit: // value for 'limit'
 *   },
 * });
 */
export function useDirectoryLogsQuery(baseOptions: Apollo.QueryHookOptions<DirectoryLogsQuery, DirectoryLogsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<DirectoryLogsQuery, DirectoryLogsQueryVariables>(DirectoryLogsDocument, options);
      }
export function useDirectoryLogsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<DirectoryLogsQuery, DirectoryLogsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<DirectoryLogsQuery, DirectoryLogsQueryVariables>(DirectoryLogsDocument, options);
        }
export type DirectoryLogsQueryHookResult = ReturnType<typeof useDirectoryLogsQuery>;
export type DirectoryLogsLazyQueryHookResult = ReturnType<typeof useDirectoryLogsLazyQuery>;
export type DirectoryLogsQueryResult = Apollo.QueryResult<DirectoryLogsQuery, DirectoryLogsQueryVariables>;
export const ProcessLogsDocument = gql`
    query processLogs($service: String!, $instance: String!, $directory: String!, $process: String!, $from: BigInt, $to: BigInt, $fromTime: Date, $toTime: Date, $levels: [String!], $find: String, $limit: Int) {
  logs(
    service: $service
    instance: $instance
    directory: $directory
    process: $process
    from: $from
    to: $to
    fromTime: $fromTime
    toTime: $toTime
    levels: $levels
    find: $find
    limit: $limit
  ) {
    sequence
    payload {
      time
      level
      unit
      message
      terminationStatus
    }
  }
}
    `;

/**
 * __useProcessLogsQuery__
 *
 * To run a query within a React component, call `useProcessLogsQuery` and pass it any options that fit your needs.
 * When your component renders, `useProcessLogsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useProcessLogsQuery({
 *   variables: {
 *      service: // value for 'service'
 *      instance: // value for 'instance'
 *      directory: // value for 'directory'
 *      process: // value for 'process'
 *      from: // value for 'from'
 *      to: // value for 'to'
 *      fromTime: // value for 'fromTime'
 *      toTime: // value for 'toTime'
 *      levels: // value for 'levels'
 *      find: // value for 'find'
 *      limit: // value for 'limit'
 *   },
 * });
 */
export function useProcessLogsQuery(baseOptions: Apollo.QueryHookOptions<ProcessLogsQuery, ProcessLogsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ProcessLogsQuery, ProcessLogsQueryVariables>(ProcessLogsDocument, options);
      }
export function useProcessLogsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ProcessLogsQuery, ProcessLogsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ProcessLogsQuery, ProcessLogsQueryVariables>(ProcessLogsDocument, options);
        }
export type ProcessLogsQueryHookResult = ReturnType<typeof useProcessLogsQuery>;
export type ProcessLogsLazyQueryHookResult = ReturnType<typeof useProcessLogsLazyQuery>;
export type ProcessLogsQueryResult = Apollo.QueryResult<ProcessLogsQuery, ProcessLogsQueryVariables>;
export const FaultDistributionsDocument = gql`
    query faultDistributions {
  faultDistributions
}
    `;

/**
 * __useFaultDistributionsQuery__
 *
 * To run a query within a React component, call `useFaultDistributionsQuery` and pass it any options that fit your needs.
 * When your component renders, `useFaultDistributionsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useFaultDistributionsQuery({
 *   variables: {
 *   },
 * });
 */
export function useFaultDistributionsQuery(baseOptions?: Apollo.QueryHookOptions<FaultDistributionsQuery, FaultDistributionsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<FaultDistributionsQuery, FaultDistributionsQueryVariables>(FaultDistributionsDocument, options);
      }
export function useFaultDistributionsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<FaultDistributionsQuery, FaultDistributionsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<FaultDistributionsQuery, FaultDistributionsQueryVariables>(FaultDistributionsDocument, options);
        }
export type FaultDistributionsQueryHookResult = ReturnType<typeof useFaultDistributionsQuery>;
export type FaultDistributionsLazyQueryHookResult = ReturnType<typeof useFaultDistributionsLazyQuery>;
export type FaultDistributionsQueryResult = Apollo.QueryResult<FaultDistributionsQuery, FaultDistributionsQueryVariables>;
export const FaultServicesDocument = gql`
    query faultServices($distribution: String) {
  faultServices(distribution: $distribution)
}
    `;

/**
 * __useFaultServicesQuery__
 *
 * To run a query within a React component, call `useFaultServicesQuery` and pass it any options that fit your needs.
 * When your component renders, `useFaultServicesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useFaultServicesQuery({
 *   variables: {
 *      distribution: // value for 'distribution'
 *   },
 * });
 */
export function useFaultServicesQuery(baseOptions?: Apollo.QueryHookOptions<FaultServicesQuery, FaultServicesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<FaultServicesQuery, FaultServicesQueryVariables>(FaultServicesDocument, options);
      }
export function useFaultServicesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<FaultServicesQuery, FaultServicesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<FaultServicesQuery, FaultServicesQueryVariables>(FaultServicesDocument, options);
        }
export type FaultServicesQueryHookResult = ReturnType<typeof useFaultServicesQuery>;
export type FaultServicesLazyQueryHookResult = ReturnType<typeof useFaultServicesLazyQuery>;
export type FaultServicesQueryResult = Apollo.QueryResult<FaultServicesQuery, FaultServicesQueryVariables>;
export const FaultsStartTimeDocument = gql`
    query faultsStartTime($distribution: String, $service: String) {
  faultsStartTime(distribution: $distribution, service: $service)
}
    `;

/**
 * __useFaultsStartTimeQuery__
 *
 * To run a query within a React component, call `useFaultsStartTimeQuery` and pass it any options that fit your needs.
 * When your component renders, `useFaultsStartTimeQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useFaultsStartTimeQuery({
 *   variables: {
 *      distribution: // value for 'distribution'
 *      service: // value for 'service'
 *   },
 * });
 */
export function useFaultsStartTimeQuery(baseOptions?: Apollo.QueryHookOptions<FaultsStartTimeQuery, FaultsStartTimeQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<FaultsStartTimeQuery, FaultsStartTimeQueryVariables>(FaultsStartTimeDocument, options);
      }
export function useFaultsStartTimeLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<FaultsStartTimeQuery, FaultsStartTimeQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<FaultsStartTimeQuery, FaultsStartTimeQueryVariables>(FaultsStartTimeDocument, options);
        }
export type FaultsStartTimeQueryHookResult = ReturnType<typeof useFaultsStartTimeQuery>;
export type FaultsStartTimeLazyQueryHookResult = ReturnType<typeof useFaultsStartTimeLazyQuery>;
export type FaultsStartTimeQueryResult = Apollo.QueryResult<FaultsStartTimeQuery, FaultsStartTimeQueryVariables>;
export const FaultsEndTimeDocument = gql`
    query faultsEndTime($distribution: String, $service: String) {
  faultsEndTime(distribution: $distribution, service: $service)
}
    `;

/**
 * __useFaultsEndTimeQuery__
 *
 * To run a query within a React component, call `useFaultsEndTimeQuery` and pass it any options that fit your needs.
 * When your component renders, `useFaultsEndTimeQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useFaultsEndTimeQuery({
 *   variables: {
 *      distribution: // value for 'distribution'
 *      service: // value for 'service'
 *   },
 * });
 */
export function useFaultsEndTimeQuery(baseOptions?: Apollo.QueryHookOptions<FaultsEndTimeQuery, FaultsEndTimeQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<FaultsEndTimeQuery, FaultsEndTimeQueryVariables>(FaultsEndTimeDocument, options);
      }
export function useFaultsEndTimeLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<FaultsEndTimeQuery, FaultsEndTimeQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<FaultsEndTimeQuery, FaultsEndTimeQueryVariables>(FaultsEndTimeDocument, options);
        }
export type FaultsEndTimeQueryHookResult = ReturnType<typeof useFaultsEndTimeQuery>;
export type FaultsEndTimeLazyQueryHookResult = ReturnType<typeof useFaultsEndTimeLazyQuery>;
export type FaultsEndTimeQueryResult = Apollo.QueryResult<FaultsEndTimeQuery, FaultsEndTimeQueryVariables>;
export const FaultsDocument = gql`
    query faults($distribution: String, $service: String, $fromTime: Date, $toTime: Date) {
  faults(
    distribution: $distribution
    service: $service
    fromTime: $fromTime
    toTime: $toTime
  ) {
    distribution
    payload {
      fault
      info {
        time
        instance
        service
        serviceDirectory
        serviceRole
        state {
          time
          installTime
          startTime
          version {
            distribution
            developerBuild
            clientBuild
          }
          updateToVersion {
            distribution
            developerBuild
            clientBuild
          }
          updateError {
            critical
            error
          }
          failuresCount
          lastExitCode
        }
        logTail {
          time
          level
          unit
          message
          terminationStatus
        }
      }
      files {
        path
        time
        length
      }
    }
  }
}
    `;

/**
 * __useFaultsQuery__
 *
 * To run a query within a React component, call `useFaultsQuery` and pass it any options that fit your needs.
 * When your component renders, `useFaultsQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useFaultsQuery({
 *   variables: {
 *      distribution: // value for 'distribution'
 *      service: // value for 'service'
 *      fromTime: // value for 'fromTime'
 *      toTime: // value for 'toTime'
 *   },
 * });
 */
export function useFaultsQuery(baseOptions?: Apollo.QueryHookOptions<FaultsQuery, FaultsQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<FaultsQuery, FaultsQueryVariables>(FaultsDocument, options);
      }
export function useFaultsLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<FaultsQuery, FaultsQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<FaultsQuery, FaultsQueryVariables>(FaultsDocument, options);
        }
export type FaultsQueryHookResult = ReturnType<typeof useFaultsQuery>;
export type FaultsLazyQueryHookResult = ReturnType<typeof useFaultsLazyQuery>;
export type FaultsQueryResult = Apollo.QueryResult<FaultsQuery, FaultsQueryVariables>;
export const FaultDocument = gql`
    query fault($fault: String!) {
  faults(fault: $fault) {
    distribution
    payload {
      fault
      info {
        time
        instance
        service
        serviceDirectory
        serviceRole
        state {
          time
          installTime
          startTime
          version {
            distribution
            developerBuild
            clientBuild
          }
          updateToVersion {
            distribution
            developerBuild
            clientBuild
          }
          updateError {
            critical
            error
          }
          failuresCount
          lastExitCode
        }
        logTail {
          time
          level
          unit
          message
          terminationStatus
        }
      }
      files {
        path
        time
        length
      }
    }
  }
}
    `;

/**
 * __useFaultQuery__
 *
 * To run a query within a React component, call `useFaultQuery` and pass it any options that fit your needs.
 * When your component renders, `useFaultQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useFaultQuery({
 *   variables: {
 *      fault: // value for 'fault'
 *   },
 * });
 */
export function useFaultQuery(baseOptions: Apollo.QueryHookOptions<FaultQuery, FaultQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<FaultQuery, FaultQueryVariables>(FaultDocument, options);
      }
export function useFaultLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<FaultQuery, FaultQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<FaultQuery, FaultQueryVariables>(FaultDocument, options);
        }
export type FaultQueryHookResult = ReturnType<typeof useFaultQuery>;
export type FaultLazyQueryHookResult = ReturnType<typeof useFaultLazyQuery>;
export type FaultQueryResult = Apollo.QueryResult<FaultQuery, FaultQueryVariables>;
export const AccountsListDocument = gql`
    query accountsList {
  accountsList
}
    `;

/**
 * __useAccountsListQuery__
 *
 * To run a query within a React component, call `useAccountsListQuery` and pass it any options that fit your needs.
 * When your component renders, `useAccountsListQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useAccountsListQuery({
 *   variables: {
 *   },
 * });
 */
export function useAccountsListQuery(baseOptions?: Apollo.QueryHookOptions<AccountsListQuery, AccountsListQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<AccountsListQuery, AccountsListQueryVariables>(AccountsListDocument, options);
      }
export function useAccountsListLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<AccountsListQuery, AccountsListQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<AccountsListQuery, AccountsListQueryVariables>(AccountsListDocument, options);
        }
export type AccountsListQueryHookResult = ReturnType<typeof useAccountsListQuery>;
export type AccountsListLazyQueryHookResult = ReturnType<typeof useAccountsListLazyQuery>;
export type AccountsListQueryResult = Apollo.QueryResult<AccountsListQuery, AccountsListQueryVariables>;
export const UserAccountsInfoDocument = gql`
    query userAccountsInfo {
  userAccountsInfo {
    account
    name
    role
    properties {
      email
      notifications
    }
  }
}
    `;

/**
 * __useUserAccountsInfoQuery__
 *
 * To run a query within a React component, call `useUserAccountsInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useUserAccountsInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useUserAccountsInfoQuery({
 *   variables: {
 *   },
 * });
 */
export function useUserAccountsInfoQuery(baseOptions?: Apollo.QueryHookOptions<UserAccountsInfoQuery, UserAccountsInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<UserAccountsInfoQuery, UserAccountsInfoQueryVariables>(UserAccountsInfoDocument, options);
      }
export function useUserAccountsInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<UserAccountsInfoQuery, UserAccountsInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<UserAccountsInfoQuery, UserAccountsInfoQueryVariables>(UserAccountsInfoDocument, options);
        }
export type UserAccountsInfoQueryHookResult = ReturnType<typeof useUserAccountsInfoQuery>;
export type UserAccountsInfoLazyQueryHookResult = ReturnType<typeof useUserAccountsInfoLazyQuery>;
export type UserAccountsInfoQueryResult = Apollo.QueryResult<UserAccountsInfoQuery, UserAccountsInfoQueryVariables>;
export const UserAccountInfoDocument = gql`
    query userAccountInfo($account: String!) {
  userAccountsInfo(account: $account) {
    account
    name
    role
    properties {
      email
      notifications
    }
  }
}
    `;

/**
 * __useUserAccountInfoQuery__
 *
 * To run a query within a React component, call `useUserAccountInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useUserAccountInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useUserAccountInfoQuery({
 *   variables: {
 *      account: // value for 'account'
 *   },
 * });
 */
export function useUserAccountInfoQuery(baseOptions: Apollo.QueryHookOptions<UserAccountInfoQuery, UserAccountInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<UserAccountInfoQuery, UserAccountInfoQueryVariables>(UserAccountInfoDocument, options);
      }
export function useUserAccountInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<UserAccountInfoQuery, UserAccountInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<UserAccountInfoQuery, UserAccountInfoQueryVariables>(UserAccountInfoDocument, options);
        }
export type UserAccountInfoQueryHookResult = ReturnType<typeof useUserAccountInfoQuery>;
export type UserAccountInfoLazyQueryHookResult = ReturnType<typeof useUserAccountInfoLazyQuery>;
export type UserAccountInfoQueryResult = Apollo.QueryResult<UserAccountInfoQuery, UserAccountInfoQueryVariables>;
export const ServiceAccountsInfoDocument = gql`
    query serviceAccountsInfo {
  serviceAccountsInfo {
    account
    name
    role
  }
}
    `;

/**
 * __useServiceAccountsInfoQuery__
 *
 * To run a query within a React component, call `useServiceAccountsInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useServiceAccountsInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useServiceAccountsInfoQuery({
 *   variables: {
 *   },
 * });
 */
export function useServiceAccountsInfoQuery(baseOptions?: Apollo.QueryHookOptions<ServiceAccountsInfoQuery, ServiceAccountsInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ServiceAccountsInfoQuery, ServiceAccountsInfoQueryVariables>(ServiceAccountsInfoDocument, options);
      }
export function useServiceAccountsInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ServiceAccountsInfoQuery, ServiceAccountsInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ServiceAccountsInfoQuery, ServiceAccountsInfoQueryVariables>(ServiceAccountsInfoDocument, options);
        }
export type ServiceAccountsInfoQueryHookResult = ReturnType<typeof useServiceAccountsInfoQuery>;
export type ServiceAccountsInfoLazyQueryHookResult = ReturnType<typeof useServiceAccountsInfoLazyQuery>;
export type ServiceAccountsInfoQueryResult = Apollo.QueryResult<ServiceAccountsInfoQuery, ServiceAccountsInfoQueryVariables>;
export const ServiceAccountInfoDocument = gql`
    query serviceAccountInfo($account: String!) {
  serviceAccountsInfo(account: $account) {
    account
    name
    role
  }
}
    `;

/**
 * __useServiceAccountInfoQuery__
 *
 * To run a query within a React component, call `useServiceAccountInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useServiceAccountInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useServiceAccountInfoQuery({
 *   variables: {
 *      account: // value for 'account'
 *   },
 * });
 */
export function useServiceAccountInfoQuery(baseOptions: Apollo.QueryHookOptions<ServiceAccountInfoQuery, ServiceAccountInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ServiceAccountInfoQuery, ServiceAccountInfoQueryVariables>(ServiceAccountInfoDocument, options);
      }
export function useServiceAccountInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ServiceAccountInfoQuery, ServiceAccountInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ServiceAccountInfoQuery, ServiceAccountInfoQueryVariables>(ServiceAccountInfoDocument, options);
        }
export type ServiceAccountInfoQueryHookResult = ReturnType<typeof useServiceAccountInfoQuery>;
export type ServiceAccountInfoLazyQueryHookResult = ReturnType<typeof useServiceAccountInfoLazyQuery>;
export type ServiceAccountInfoQueryResult = Apollo.QueryResult<ServiceAccountInfoQuery, ServiceAccountInfoQueryVariables>;
export const ConsumerAccountsInfoDocument = gql`
    query consumerAccountsInfo {
  consumerAccountsInfo {
    account
    name
    role
    properties {
      url
      profile
    }
  }
}
    `;

/**
 * __useConsumerAccountsInfoQuery__
 *
 * To run a query within a React component, call `useConsumerAccountsInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useConsumerAccountsInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useConsumerAccountsInfoQuery({
 *   variables: {
 *   },
 * });
 */
export function useConsumerAccountsInfoQuery(baseOptions?: Apollo.QueryHookOptions<ConsumerAccountsInfoQuery, ConsumerAccountsInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ConsumerAccountsInfoQuery, ConsumerAccountsInfoQueryVariables>(ConsumerAccountsInfoDocument, options);
      }
export function useConsumerAccountsInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ConsumerAccountsInfoQuery, ConsumerAccountsInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ConsumerAccountsInfoQuery, ConsumerAccountsInfoQueryVariables>(ConsumerAccountsInfoDocument, options);
        }
export type ConsumerAccountsInfoQueryHookResult = ReturnType<typeof useConsumerAccountsInfoQuery>;
export type ConsumerAccountsInfoLazyQueryHookResult = ReturnType<typeof useConsumerAccountsInfoLazyQuery>;
export type ConsumerAccountsInfoQueryResult = Apollo.QueryResult<ConsumerAccountsInfoQuery, ConsumerAccountsInfoQueryVariables>;
export const ConsumerAccountInfoDocument = gql`
    query consumerAccountInfo($account: String!) {
  consumerAccountsInfo(account: $account) {
    account
    name
    role
    properties {
      url
      profile
    }
  }
}
    `;

/**
 * __useConsumerAccountInfoQuery__
 *
 * To run a query within a React component, call `useConsumerAccountInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useConsumerAccountInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useConsumerAccountInfoQuery({
 *   variables: {
 *      account: // value for 'account'
 *   },
 * });
 */
export function useConsumerAccountInfoQuery(baseOptions: Apollo.QueryHookOptions<ConsumerAccountInfoQuery, ConsumerAccountInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ConsumerAccountInfoQuery, ConsumerAccountInfoQueryVariables>(ConsumerAccountInfoDocument, options);
      }
export function useConsumerAccountInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ConsumerAccountInfoQuery, ConsumerAccountInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ConsumerAccountInfoQuery, ConsumerAccountInfoQueryVariables>(ConsumerAccountInfoDocument, options);
        }
export type ConsumerAccountInfoQueryHookResult = ReturnType<typeof useConsumerAccountInfoQuery>;
export type ConsumerAccountInfoLazyQueryHookResult = ReturnType<typeof useConsumerAccountInfoLazyQuery>;
export type ConsumerAccountInfoQueryResult = Apollo.QueryResult<ConsumerAccountInfoQuery, ConsumerAccountInfoQueryVariables>;
export const AccessTokenDocument = gql`
    query accessToken($account: String!) {
  accessToken(account: $account)
}
    `;

/**
 * __useAccessTokenQuery__
 *
 * To run a query within a React component, call `useAccessTokenQuery` and pass it any options that fit your needs.
 * When your component renders, `useAccessTokenQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useAccessTokenQuery({
 *   variables: {
 *      account: // value for 'account'
 *   },
 * });
 */
export function useAccessTokenQuery(baseOptions: Apollo.QueryHookOptions<AccessTokenQuery, AccessTokenQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<AccessTokenQuery, AccessTokenQueryVariables>(AccessTokenDocument, options);
      }
export function useAccessTokenLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<AccessTokenQuery, AccessTokenQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<AccessTokenQuery, AccessTokenQueryVariables>(AccessTokenDocument, options);
        }
export type AccessTokenQueryHookResult = ReturnType<typeof useAccessTokenQuery>;
export type AccessTokenLazyQueryHookResult = ReturnType<typeof useAccessTokenLazyQuery>;
export type AccessTokenQueryResult = Apollo.QueryResult<AccessTokenQuery, AccessTokenQueryVariables>;
export const AddUserAccountDocument = gql`
    mutation addUserAccount($account: String!, $name: String!, $role: AccountRole!, $password: String!, $properties: UserAccountPropertiesInput!) {
  addUserAccount(
    account: $account
    name: $name
    role: $role
    password: $password
    properties: $properties
  )
}
    `;
export type AddUserAccountMutationFn = Apollo.MutationFunction<AddUserAccountMutation, AddUserAccountMutationVariables>;

/**
 * __useAddUserAccountMutation__
 *
 * To run a mutation, you first call `useAddUserAccountMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useAddUserAccountMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [addUserAccountMutation, { data, loading, error }] = useAddUserAccountMutation({
 *   variables: {
 *      account: // value for 'account'
 *      name: // value for 'name'
 *      role: // value for 'role'
 *      password: // value for 'password'
 *      properties: // value for 'properties'
 *   },
 * });
 */
export function useAddUserAccountMutation(baseOptions?: Apollo.MutationHookOptions<AddUserAccountMutation, AddUserAccountMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<AddUserAccountMutation, AddUserAccountMutationVariables>(AddUserAccountDocument, options);
      }
export type AddUserAccountMutationHookResult = ReturnType<typeof useAddUserAccountMutation>;
export type AddUserAccountMutationResult = Apollo.MutationResult<AddUserAccountMutation>;
export type AddUserAccountMutationOptions = Apollo.BaseMutationOptions<AddUserAccountMutation, AddUserAccountMutationVariables>;
export const ChangeUserAccountDocument = gql`
    mutation changeUserAccount($account: String!, $name: String, $role: AccountRole!, $oldPassword: String, $password: String, $properties: UserAccountPropertiesInput) {
  changeUserAccount(
    account: $account
    name: $name
    role: $role
    oldPassword: $oldPassword
    password: $password
    properties: $properties
  )
}
    `;
export type ChangeUserAccountMutationFn = Apollo.MutationFunction<ChangeUserAccountMutation, ChangeUserAccountMutationVariables>;

/**
 * __useChangeUserAccountMutation__
 *
 * To run a mutation, you first call `useChangeUserAccountMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useChangeUserAccountMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [changeUserAccountMutation, { data, loading, error }] = useChangeUserAccountMutation({
 *   variables: {
 *      account: // value for 'account'
 *      name: // value for 'name'
 *      role: // value for 'role'
 *      oldPassword: // value for 'oldPassword'
 *      password: // value for 'password'
 *      properties: // value for 'properties'
 *   },
 * });
 */
export function useChangeUserAccountMutation(baseOptions?: Apollo.MutationHookOptions<ChangeUserAccountMutation, ChangeUserAccountMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<ChangeUserAccountMutation, ChangeUserAccountMutationVariables>(ChangeUserAccountDocument, options);
      }
export type ChangeUserAccountMutationHookResult = ReturnType<typeof useChangeUserAccountMutation>;
export type ChangeUserAccountMutationResult = Apollo.MutationResult<ChangeUserAccountMutation>;
export type ChangeUserAccountMutationOptions = Apollo.BaseMutationOptions<ChangeUserAccountMutation, ChangeUserAccountMutationVariables>;
export const AddServiceAccountDocument = gql`
    mutation addServiceAccount($account: String!, $name: String!, $role: AccountRole!) {
  addServiceAccount(account: $account, name: $name, role: $role)
}
    `;
export type AddServiceAccountMutationFn = Apollo.MutationFunction<AddServiceAccountMutation, AddServiceAccountMutationVariables>;

/**
 * __useAddServiceAccountMutation__
 *
 * To run a mutation, you first call `useAddServiceAccountMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useAddServiceAccountMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [addServiceAccountMutation, { data, loading, error }] = useAddServiceAccountMutation({
 *   variables: {
 *      account: // value for 'account'
 *      name: // value for 'name'
 *      role: // value for 'role'
 *   },
 * });
 */
export function useAddServiceAccountMutation(baseOptions?: Apollo.MutationHookOptions<AddServiceAccountMutation, AddServiceAccountMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<AddServiceAccountMutation, AddServiceAccountMutationVariables>(AddServiceAccountDocument, options);
      }
export type AddServiceAccountMutationHookResult = ReturnType<typeof useAddServiceAccountMutation>;
export type AddServiceAccountMutationResult = Apollo.MutationResult<AddServiceAccountMutation>;
export type AddServiceAccountMutationOptions = Apollo.BaseMutationOptions<AddServiceAccountMutation, AddServiceAccountMutationVariables>;
export const ChangeServiceAccountDocument = gql`
    mutation changeServiceAccount($account: String!, $name: String, $role: AccountRole!) {
  changeServiceAccount(account: $account, name: $name, role: $role)
}
    `;
export type ChangeServiceAccountMutationFn = Apollo.MutationFunction<ChangeServiceAccountMutation, ChangeServiceAccountMutationVariables>;

/**
 * __useChangeServiceAccountMutation__
 *
 * To run a mutation, you first call `useChangeServiceAccountMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useChangeServiceAccountMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [changeServiceAccountMutation, { data, loading, error }] = useChangeServiceAccountMutation({
 *   variables: {
 *      account: // value for 'account'
 *      name: // value for 'name'
 *      role: // value for 'role'
 *   },
 * });
 */
export function useChangeServiceAccountMutation(baseOptions?: Apollo.MutationHookOptions<ChangeServiceAccountMutation, ChangeServiceAccountMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<ChangeServiceAccountMutation, ChangeServiceAccountMutationVariables>(ChangeServiceAccountDocument, options);
      }
export type ChangeServiceAccountMutationHookResult = ReturnType<typeof useChangeServiceAccountMutation>;
export type ChangeServiceAccountMutationResult = Apollo.MutationResult<ChangeServiceAccountMutation>;
export type ChangeServiceAccountMutationOptions = Apollo.BaseMutationOptions<ChangeServiceAccountMutation, ChangeServiceAccountMutationVariables>;
export const AddConsumerAccountDocument = gql`
    mutation addConsumerAccount($account: String!, $name: String!, $properties: ConsumerAccountPropertiesInput!) {
  addConsumerAccount(account: $account, name: $name, properties: $properties)
}
    `;
export type AddConsumerAccountMutationFn = Apollo.MutationFunction<AddConsumerAccountMutation, AddConsumerAccountMutationVariables>;

/**
 * __useAddConsumerAccountMutation__
 *
 * To run a mutation, you first call `useAddConsumerAccountMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useAddConsumerAccountMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [addConsumerAccountMutation, { data, loading, error }] = useAddConsumerAccountMutation({
 *   variables: {
 *      account: // value for 'account'
 *      name: // value for 'name'
 *      properties: // value for 'properties'
 *   },
 * });
 */
export function useAddConsumerAccountMutation(baseOptions?: Apollo.MutationHookOptions<AddConsumerAccountMutation, AddConsumerAccountMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<AddConsumerAccountMutation, AddConsumerAccountMutationVariables>(AddConsumerAccountDocument, options);
      }
export type AddConsumerAccountMutationHookResult = ReturnType<typeof useAddConsumerAccountMutation>;
export type AddConsumerAccountMutationResult = Apollo.MutationResult<AddConsumerAccountMutation>;
export type AddConsumerAccountMutationOptions = Apollo.BaseMutationOptions<AddConsumerAccountMutation, AddConsumerAccountMutationVariables>;
export const ChangeConsumerAccountDocument = gql`
    mutation changeConsumerAccount($account: String!, $name: String, $properties: ConsumerAccountPropertiesInput) {
  changeConsumerAccount(account: $account, name: $name, properties: $properties)
}
    `;
export type ChangeConsumerAccountMutationFn = Apollo.MutationFunction<ChangeConsumerAccountMutation, ChangeConsumerAccountMutationVariables>;

/**
 * __useChangeConsumerAccountMutation__
 *
 * To run a mutation, you first call `useChangeConsumerAccountMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useChangeConsumerAccountMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [changeConsumerAccountMutation, { data, loading, error }] = useChangeConsumerAccountMutation({
 *   variables: {
 *      account: // value for 'account'
 *      name: // value for 'name'
 *      properties: // value for 'properties'
 *   },
 * });
 */
export function useChangeConsumerAccountMutation(baseOptions?: Apollo.MutationHookOptions<ChangeConsumerAccountMutation, ChangeConsumerAccountMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<ChangeConsumerAccountMutation, ChangeConsumerAccountMutationVariables>(ChangeConsumerAccountDocument, options);
      }
export type ChangeConsumerAccountMutationHookResult = ReturnType<typeof useChangeConsumerAccountMutation>;
export type ChangeConsumerAccountMutationResult = Apollo.MutationResult<ChangeConsumerAccountMutation>;
export type ChangeConsumerAccountMutationOptions = Apollo.BaseMutationOptions<ChangeConsumerAccountMutation, ChangeConsumerAccountMutationVariables>;
export const RemoveAccountDocument = gql`
    mutation removeAccount($account: String!) {
  removeAccount(account: $account)
}
    `;
export type RemoveAccountMutationFn = Apollo.MutationFunction<RemoveAccountMutation, RemoveAccountMutationVariables>;

/**
 * __useRemoveAccountMutation__
 *
 * To run a mutation, you first call `useRemoveAccountMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useRemoveAccountMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [removeAccountMutation, { data, loading, error }] = useRemoveAccountMutation({
 *   variables: {
 *      account: // value for 'account'
 *   },
 * });
 */
export function useRemoveAccountMutation(baseOptions?: Apollo.MutationHookOptions<RemoveAccountMutation, RemoveAccountMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<RemoveAccountMutation, RemoveAccountMutationVariables>(RemoveAccountDocument, options);
      }
export type RemoveAccountMutationHookResult = ReturnType<typeof useRemoveAccountMutation>;
export type RemoveAccountMutationResult = Apollo.MutationResult<RemoveAccountMutation>;
export type RemoveAccountMutationOptions = Apollo.BaseMutationOptions<RemoveAccountMutation, RemoveAccountMutationVariables>;
export const BuildDeveloperServicesDocument = gql`
    query buildDeveloperServices {
  buildDeveloperServicesConfig {
    service
  }
}
    `;

/**
 * __useBuildDeveloperServicesQuery__
 *
 * To run a query within a React component, call `useBuildDeveloperServicesQuery` and pass it any options that fit your needs.
 * When your component renders, `useBuildDeveloperServicesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useBuildDeveloperServicesQuery({
 *   variables: {
 *   },
 * });
 */
export function useBuildDeveloperServicesQuery(baseOptions?: Apollo.QueryHookOptions<BuildDeveloperServicesQuery, BuildDeveloperServicesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<BuildDeveloperServicesQuery, BuildDeveloperServicesQueryVariables>(BuildDeveloperServicesDocument, options);
      }
export function useBuildDeveloperServicesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<BuildDeveloperServicesQuery, BuildDeveloperServicesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<BuildDeveloperServicesQuery, BuildDeveloperServicesQueryVariables>(BuildDeveloperServicesDocument, options);
        }
export type BuildDeveloperServicesQueryHookResult = ReturnType<typeof useBuildDeveloperServicesQuery>;
export type BuildDeveloperServicesLazyQueryHookResult = ReturnType<typeof useBuildDeveloperServicesLazyQuery>;
export type BuildDeveloperServicesQueryResult = Apollo.QueryResult<BuildDeveloperServicesQuery, BuildDeveloperServicesQueryVariables>;
export const BuildDeveloperServiceConfigDocument = gql`
    query buildDeveloperServiceConfig($service: String!) {
  buildDeveloperServicesConfig(service: $service) {
    service
    distribution
    environment {
      name
      value
    }
    repositories {
      name
      git {
        url
        branch
        cloneSubmodules
      }
      subDirectory
    }
    privateFiles {
      path
      time
      length
    }
    macroValues {
      name
      value
    }
  }
}
    `;

/**
 * __useBuildDeveloperServiceConfigQuery__
 *
 * To run a query within a React component, call `useBuildDeveloperServiceConfigQuery` and pass it any options that fit your needs.
 * When your component renders, `useBuildDeveloperServiceConfigQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useBuildDeveloperServiceConfigQuery({
 *   variables: {
 *      service: // value for 'service'
 *   },
 * });
 */
export function useBuildDeveloperServiceConfigQuery(baseOptions: Apollo.QueryHookOptions<BuildDeveloperServiceConfigQuery, BuildDeveloperServiceConfigQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<BuildDeveloperServiceConfigQuery, BuildDeveloperServiceConfigQueryVariables>(BuildDeveloperServiceConfigDocument, options);
      }
export function useBuildDeveloperServiceConfigLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<BuildDeveloperServiceConfigQuery, BuildDeveloperServiceConfigQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<BuildDeveloperServiceConfigQuery, BuildDeveloperServiceConfigQueryVariables>(BuildDeveloperServiceConfigDocument, options);
        }
export type BuildDeveloperServiceConfigQueryHookResult = ReturnType<typeof useBuildDeveloperServiceConfigQuery>;
export type BuildDeveloperServiceConfigLazyQueryHookResult = ReturnType<typeof useBuildDeveloperServiceConfigLazyQuery>;
export type BuildDeveloperServiceConfigQueryResult = Apollo.QueryResult<BuildDeveloperServiceConfigQuery, BuildDeveloperServiceConfigQueryVariables>;
export const BuildClientServicesDocument = gql`
    query buildClientServices {
  buildClientServicesConfig {
    service
  }
}
    `;

/**
 * __useBuildClientServicesQuery__
 *
 * To run a query within a React component, call `useBuildClientServicesQuery` and pass it any options that fit your needs.
 * When your component renders, `useBuildClientServicesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useBuildClientServicesQuery({
 *   variables: {
 *   },
 * });
 */
export function useBuildClientServicesQuery(baseOptions?: Apollo.QueryHookOptions<BuildClientServicesQuery, BuildClientServicesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<BuildClientServicesQuery, BuildClientServicesQueryVariables>(BuildClientServicesDocument, options);
      }
export function useBuildClientServicesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<BuildClientServicesQuery, BuildClientServicesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<BuildClientServicesQuery, BuildClientServicesQueryVariables>(BuildClientServicesDocument, options);
        }
export type BuildClientServicesQueryHookResult = ReturnType<typeof useBuildClientServicesQuery>;
export type BuildClientServicesLazyQueryHookResult = ReturnType<typeof useBuildClientServicesLazyQuery>;
export type BuildClientServicesQueryResult = Apollo.QueryResult<BuildClientServicesQuery, BuildClientServicesQueryVariables>;
export const BuildClientServiceConfigDocument = gql`
    query buildClientServiceConfig($service: String!) {
  buildClientServicesConfig(service: $service) {
    service
    distribution
    environment {
      name
      value
    }
    repositories {
      name
      git {
        url
        branch
        cloneSubmodules
      }
      subDirectory
    }
    privateFiles {
      path
      time
      length
    }
    macroValues {
      name
      value
    }
  }
}
    `;

/**
 * __useBuildClientServiceConfigQuery__
 *
 * To run a query within a React component, call `useBuildClientServiceConfigQuery` and pass it any options that fit your needs.
 * When your component renders, `useBuildClientServiceConfigQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useBuildClientServiceConfigQuery({
 *   variables: {
 *      service: // value for 'service'
 *   },
 * });
 */
export function useBuildClientServiceConfigQuery(baseOptions: Apollo.QueryHookOptions<BuildClientServiceConfigQuery, BuildClientServiceConfigQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<BuildClientServiceConfigQuery, BuildClientServiceConfigQueryVariables>(BuildClientServiceConfigDocument, options);
      }
export function useBuildClientServiceConfigLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<BuildClientServiceConfigQuery, BuildClientServiceConfigQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<BuildClientServiceConfigQuery, BuildClientServiceConfigQueryVariables>(BuildClientServiceConfigDocument, options);
        }
export type BuildClientServiceConfigQueryHookResult = ReturnType<typeof useBuildClientServiceConfigQuery>;
export type BuildClientServiceConfigLazyQueryHookResult = ReturnType<typeof useBuildClientServiceConfigLazyQuery>;
export type BuildClientServiceConfigQueryResult = Apollo.QueryResult<BuildClientServiceConfigQuery, BuildClientServiceConfigQueryVariables>;
export const SetBuildDeveloperServiceConfigDocument = gql`
    mutation setBuildDeveloperServiceConfig($service: String!, $distribution: String, $environment: [NamedStringValueInput!]!, $repositories: [RepositoryInput!]!, $privateFiles: [FileInfoInput!]!, $macroValues: [NamedStringValueInput!]!) {
  setBuildDeveloperServiceConfig(
    service: $service
    distribution: $distribution
    environment: $environment
    repositories: $repositories
    privateFiles: $privateFiles
    macroValues: $macroValues
  )
}
    `;
export type SetBuildDeveloperServiceConfigMutationFn = Apollo.MutationFunction<SetBuildDeveloperServiceConfigMutation, SetBuildDeveloperServiceConfigMutationVariables>;

/**
 * __useSetBuildDeveloperServiceConfigMutation__
 *
 * To run a mutation, you first call `useSetBuildDeveloperServiceConfigMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useSetBuildDeveloperServiceConfigMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [setBuildDeveloperServiceConfigMutation, { data, loading, error }] = useSetBuildDeveloperServiceConfigMutation({
 *   variables: {
 *      service: // value for 'service'
 *      distribution: // value for 'distribution'
 *      environment: // value for 'environment'
 *      repositories: // value for 'repositories'
 *      privateFiles: // value for 'privateFiles'
 *      macroValues: // value for 'macroValues'
 *   },
 * });
 */
export function useSetBuildDeveloperServiceConfigMutation(baseOptions?: Apollo.MutationHookOptions<SetBuildDeveloperServiceConfigMutation, SetBuildDeveloperServiceConfigMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<SetBuildDeveloperServiceConfigMutation, SetBuildDeveloperServiceConfigMutationVariables>(SetBuildDeveloperServiceConfigDocument, options);
      }
export type SetBuildDeveloperServiceConfigMutationHookResult = ReturnType<typeof useSetBuildDeveloperServiceConfigMutation>;
export type SetBuildDeveloperServiceConfigMutationResult = Apollo.MutationResult<SetBuildDeveloperServiceConfigMutation>;
export type SetBuildDeveloperServiceConfigMutationOptions = Apollo.BaseMutationOptions<SetBuildDeveloperServiceConfigMutation, SetBuildDeveloperServiceConfigMutationVariables>;
export const RemoveBuildDeveloperServiceConfigDocument = gql`
    mutation removeBuildDeveloperServiceConfig($service: String!) {
  removeBuildDeveloperServiceConfig(service: $service)
}
    `;
export type RemoveBuildDeveloperServiceConfigMutationFn = Apollo.MutationFunction<RemoveBuildDeveloperServiceConfigMutation, RemoveBuildDeveloperServiceConfigMutationVariables>;

/**
 * __useRemoveBuildDeveloperServiceConfigMutation__
 *
 * To run a mutation, you first call `useRemoveBuildDeveloperServiceConfigMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useRemoveBuildDeveloperServiceConfigMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [removeBuildDeveloperServiceConfigMutation, { data, loading, error }] = useRemoveBuildDeveloperServiceConfigMutation({
 *   variables: {
 *      service: // value for 'service'
 *   },
 * });
 */
export function useRemoveBuildDeveloperServiceConfigMutation(baseOptions?: Apollo.MutationHookOptions<RemoveBuildDeveloperServiceConfigMutation, RemoveBuildDeveloperServiceConfigMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<RemoveBuildDeveloperServiceConfigMutation, RemoveBuildDeveloperServiceConfigMutationVariables>(RemoveBuildDeveloperServiceConfigDocument, options);
      }
export type RemoveBuildDeveloperServiceConfigMutationHookResult = ReturnType<typeof useRemoveBuildDeveloperServiceConfigMutation>;
export type RemoveBuildDeveloperServiceConfigMutationResult = Apollo.MutationResult<RemoveBuildDeveloperServiceConfigMutation>;
export type RemoveBuildDeveloperServiceConfigMutationOptions = Apollo.BaseMutationOptions<RemoveBuildDeveloperServiceConfigMutation, RemoveBuildDeveloperServiceConfigMutationVariables>;
export const SetBuildClientServiceConfigDocument = gql`
    mutation setBuildClientServiceConfig($service: String!, $distribution: String, $environment: [NamedStringValueInput!]!, $repositories: [RepositoryInput!]!, $privateFiles: [FileInfoInput!]!, $macroValues: [NamedStringValueInput!]!) {
  setBuildClientServiceConfig(
    service: $service
    distribution: $distribution
    environment: $environment
    repositories: $repositories
    privateFiles: $privateFiles
    macroValues: $macroValues
  )
}
    `;
export type SetBuildClientServiceConfigMutationFn = Apollo.MutationFunction<SetBuildClientServiceConfigMutation, SetBuildClientServiceConfigMutationVariables>;

/**
 * __useSetBuildClientServiceConfigMutation__
 *
 * To run a mutation, you first call `useSetBuildClientServiceConfigMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useSetBuildClientServiceConfigMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [setBuildClientServiceConfigMutation, { data, loading, error }] = useSetBuildClientServiceConfigMutation({
 *   variables: {
 *      service: // value for 'service'
 *      distribution: // value for 'distribution'
 *      environment: // value for 'environment'
 *      repositories: // value for 'repositories'
 *      privateFiles: // value for 'privateFiles'
 *      macroValues: // value for 'macroValues'
 *   },
 * });
 */
export function useSetBuildClientServiceConfigMutation(baseOptions?: Apollo.MutationHookOptions<SetBuildClientServiceConfigMutation, SetBuildClientServiceConfigMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<SetBuildClientServiceConfigMutation, SetBuildClientServiceConfigMutationVariables>(SetBuildClientServiceConfigDocument, options);
      }
export type SetBuildClientServiceConfigMutationHookResult = ReturnType<typeof useSetBuildClientServiceConfigMutation>;
export type SetBuildClientServiceConfigMutationResult = Apollo.MutationResult<SetBuildClientServiceConfigMutation>;
export type SetBuildClientServiceConfigMutationOptions = Apollo.BaseMutationOptions<SetBuildClientServiceConfigMutation, SetBuildClientServiceConfigMutationVariables>;
export const RemoveBuildClientServiceConfigDocument = gql`
    mutation removeBuildClientServiceConfig($service: String!) {
  removeBuildClientServiceConfig(service: $service)
}
    `;
export type RemoveBuildClientServiceConfigMutationFn = Apollo.MutationFunction<RemoveBuildClientServiceConfigMutation, RemoveBuildClientServiceConfigMutationVariables>;

/**
 * __useRemoveBuildClientServiceConfigMutation__
 *
 * To run a mutation, you first call `useRemoveBuildClientServiceConfigMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useRemoveBuildClientServiceConfigMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [removeBuildClientServiceConfigMutation, { data, loading, error }] = useRemoveBuildClientServiceConfigMutation({
 *   variables: {
 *      service: // value for 'service'
 *   },
 * });
 */
export function useRemoveBuildClientServiceConfigMutation(baseOptions?: Apollo.MutationHookOptions<RemoveBuildClientServiceConfigMutation, RemoveBuildClientServiceConfigMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<RemoveBuildClientServiceConfigMutation, RemoveBuildClientServiceConfigMutationVariables>(RemoveBuildClientServiceConfigDocument, options);
      }
export type RemoveBuildClientServiceConfigMutationHookResult = ReturnType<typeof useRemoveBuildClientServiceConfigMutation>;
export type RemoveBuildClientServiceConfigMutationResult = Apollo.MutationResult<RemoveBuildClientServiceConfigMutation>;
export type RemoveBuildClientServiceConfigMutationOptions = Apollo.BaseMutationOptions<RemoveBuildClientServiceConfigMutation, RemoveBuildClientServiceConfigMutationVariables>;
export const ServiceProfilesDocument = gql`
    query serviceProfiles {
  serviceProfiles {
    profile
  }
}
    `;

/**
 * __useServiceProfilesQuery__
 *
 * To run a query within a React component, call `useServiceProfilesQuery` and pass it any options that fit your needs.
 * When your component renders, `useServiceProfilesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useServiceProfilesQuery({
 *   variables: {
 *   },
 * });
 */
export function useServiceProfilesQuery(baseOptions?: Apollo.QueryHookOptions<ServiceProfilesQuery, ServiceProfilesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ServiceProfilesQuery, ServiceProfilesQueryVariables>(ServiceProfilesDocument, options);
      }
export function useServiceProfilesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ServiceProfilesQuery, ServiceProfilesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ServiceProfilesQuery, ServiceProfilesQueryVariables>(ServiceProfilesDocument, options);
        }
export type ServiceProfilesQueryHookResult = ReturnType<typeof useServiceProfilesQuery>;
export type ServiceProfilesLazyQueryHookResult = ReturnType<typeof useServiceProfilesLazyQuery>;
export type ServiceProfilesQueryResult = Apollo.QueryResult<ServiceProfilesQuery, ServiceProfilesQueryVariables>;
export const ProfileServicesDocument = gql`
    query profileServices($profile: String!) {
  serviceProfiles(profile: $profile) {
    services
  }
}
    `;

/**
 * __useProfileServicesQuery__
 *
 * To run a query within a React component, call `useProfileServicesQuery` and pass it any options that fit your needs.
 * When your component renders, `useProfileServicesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useProfileServicesQuery({
 *   variables: {
 *      profile: // value for 'profile'
 *   },
 * });
 */
export function useProfileServicesQuery(baseOptions: Apollo.QueryHookOptions<ProfileServicesQuery, ProfileServicesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ProfileServicesQuery, ProfileServicesQueryVariables>(ProfileServicesDocument, options);
      }
export function useProfileServicesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ProfileServicesQuery, ProfileServicesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ProfileServicesQuery, ProfileServicesQueryVariables>(ProfileServicesDocument, options);
        }
export type ProfileServicesQueryHookResult = ReturnType<typeof useProfileServicesQuery>;
export type ProfileServicesLazyQueryHookResult = ReturnType<typeof useProfileServicesLazyQuery>;
export type ProfileServicesQueryResult = Apollo.QueryResult<ProfileServicesQuery, ProfileServicesQueryVariables>;
export const AddServicesProfileDocument = gql`
    mutation addServicesProfile($profile: String!, $services: [String!]!) {
  addServicesProfile(profile: $profile, services: $services)
}
    `;
export type AddServicesProfileMutationFn = Apollo.MutationFunction<AddServicesProfileMutation, AddServicesProfileMutationVariables>;

/**
 * __useAddServicesProfileMutation__
 *
 * To run a mutation, you first call `useAddServicesProfileMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useAddServicesProfileMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [addServicesProfileMutation, { data, loading, error }] = useAddServicesProfileMutation({
 *   variables: {
 *      profile: // value for 'profile'
 *      services: // value for 'services'
 *   },
 * });
 */
export function useAddServicesProfileMutation(baseOptions?: Apollo.MutationHookOptions<AddServicesProfileMutation, AddServicesProfileMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<AddServicesProfileMutation, AddServicesProfileMutationVariables>(AddServicesProfileDocument, options);
      }
export type AddServicesProfileMutationHookResult = ReturnType<typeof useAddServicesProfileMutation>;
export type AddServicesProfileMutationResult = Apollo.MutationResult<AddServicesProfileMutation>;
export type AddServicesProfileMutationOptions = Apollo.BaseMutationOptions<AddServicesProfileMutation, AddServicesProfileMutationVariables>;
export const ChangeServicesProfileDocument = gql`
    mutation changeServicesProfile($profile: String!, $services: [String!]!) {
  changeServicesProfile(profile: $profile, services: $services)
}
    `;
export type ChangeServicesProfileMutationFn = Apollo.MutationFunction<ChangeServicesProfileMutation, ChangeServicesProfileMutationVariables>;

/**
 * __useChangeServicesProfileMutation__
 *
 * To run a mutation, you first call `useChangeServicesProfileMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useChangeServicesProfileMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [changeServicesProfileMutation, { data, loading, error }] = useChangeServicesProfileMutation({
 *   variables: {
 *      profile: // value for 'profile'
 *      services: // value for 'services'
 *   },
 * });
 */
export function useChangeServicesProfileMutation(baseOptions?: Apollo.MutationHookOptions<ChangeServicesProfileMutation, ChangeServicesProfileMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<ChangeServicesProfileMutation, ChangeServicesProfileMutationVariables>(ChangeServicesProfileDocument, options);
      }
export type ChangeServicesProfileMutationHookResult = ReturnType<typeof useChangeServicesProfileMutation>;
export type ChangeServicesProfileMutationResult = Apollo.MutationResult<ChangeServicesProfileMutation>;
export type ChangeServicesProfileMutationOptions = Apollo.BaseMutationOptions<ChangeServicesProfileMutation, ChangeServicesProfileMutationVariables>;
export const RemoveServicesProfileDocument = gql`
    mutation removeServicesProfile($profile: String!) {
  removeServicesProfile(profile: $profile)
}
    `;
export type RemoveServicesProfileMutationFn = Apollo.MutationFunction<RemoveServicesProfileMutation, RemoveServicesProfileMutationVariables>;

/**
 * __useRemoveServicesProfileMutation__
 *
 * To run a mutation, you first call `useRemoveServicesProfileMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useRemoveServicesProfileMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [removeServicesProfileMutation, { data, loading, error }] = useRemoveServicesProfileMutation({
 *   variables: {
 *      profile: // value for 'profile'
 *   },
 * });
 */
export function useRemoveServicesProfileMutation(baseOptions?: Apollo.MutationHookOptions<RemoveServicesProfileMutation, RemoveServicesProfileMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<RemoveServicesProfileMutation, RemoveServicesProfileMutationVariables>(RemoveServicesProfileDocument, options);
      }
export type RemoveServicesProfileMutationHookResult = ReturnType<typeof useRemoveServicesProfileMutation>;
export type RemoveServicesProfileMutationResult = Apollo.MutationResult<RemoveServicesProfileMutation>;
export type RemoveServicesProfileMutationOptions = Apollo.BaseMutationOptions<RemoveServicesProfileMutation, RemoveServicesProfileMutationVariables>;
export const ProvidersInfoDocument = gql`
    query providersInfo {
  providersInfo {
    distribution
    url
    accessToken
    testConsumer
    uploadState
    autoUpdate
  }
}
    `;

/**
 * __useProvidersInfoQuery__
 *
 * To run a query within a React component, call `useProvidersInfoQuery` and pass it any options that fit your needs.
 * When your component renders, `useProvidersInfoQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useProvidersInfoQuery({
 *   variables: {
 *   },
 * });
 */
export function useProvidersInfoQuery(baseOptions?: Apollo.QueryHookOptions<ProvidersInfoQuery, ProvidersInfoQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<ProvidersInfoQuery, ProvidersInfoQueryVariables>(ProvidersInfoDocument, options);
      }
export function useProvidersInfoLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<ProvidersInfoQuery, ProvidersInfoQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<ProvidersInfoQuery, ProvidersInfoQueryVariables>(ProvidersInfoDocument, options);
        }
export type ProvidersInfoQueryHookResult = ReturnType<typeof useProvidersInfoQuery>;
export type ProvidersInfoLazyQueryHookResult = ReturnType<typeof useProvidersInfoLazyQuery>;
export type ProvidersInfoQueryResult = Apollo.QueryResult<ProvidersInfoQuery, ProvidersInfoQueryVariables>;
export const AddProviderDocument = gql`
    mutation addProvider($distribution: String!, $url: String!, $accessToken: String!, $testConsumer: String, $uploadState: Boolean!, $autoUpdate: Boolean!) {
  addProvider(
    distribution: $distribution
    url: $url
    accessToken: $accessToken
    testConsumer: $testConsumer
    uploadState: $uploadState
    autoUpdate: $autoUpdate
  )
}
    `;
export type AddProviderMutationFn = Apollo.MutationFunction<AddProviderMutation, AddProviderMutationVariables>;

/**
 * __useAddProviderMutation__
 *
 * To run a mutation, you first call `useAddProviderMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useAddProviderMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [addProviderMutation, { data, loading, error }] = useAddProviderMutation({
 *   variables: {
 *      distribution: // value for 'distribution'
 *      url: // value for 'url'
 *      accessToken: // value for 'accessToken'
 *      testConsumer: // value for 'testConsumer'
 *      uploadState: // value for 'uploadState'
 *      autoUpdate: // value for 'autoUpdate'
 *   },
 * });
 */
export function useAddProviderMutation(baseOptions?: Apollo.MutationHookOptions<AddProviderMutation, AddProviderMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<AddProviderMutation, AddProviderMutationVariables>(AddProviderDocument, options);
      }
export type AddProviderMutationHookResult = ReturnType<typeof useAddProviderMutation>;
export type AddProviderMutationResult = Apollo.MutationResult<AddProviderMutation>;
export type AddProviderMutationOptions = Apollo.BaseMutationOptions<AddProviderMutation, AddProviderMutationVariables>;
export const ChangeProviderDocument = gql`
    mutation changeProvider($distribution: String!, $url: String!, $accessToken: String!, $testConsumer: String, $uploadState: Boolean!, $autoUpdate: Boolean!) {
  changeProvider(
    distribution: $distribution
    url: $url
    accessToken: $accessToken
    testConsumer: $testConsumer
    uploadState: $uploadState
    autoUpdate: $autoUpdate
  )
}
    `;
export type ChangeProviderMutationFn = Apollo.MutationFunction<ChangeProviderMutation, ChangeProviderMutationVariables>;

/**
 * __useChangeProviderMutation__
 *
 * To run a mutation, you first call `useChangeProviderMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useChangeProviderMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [changeProviderMutation, { data, loading, error }] = useChangeProviderMutation({
 *   variables: {
 *      distribution: // value for 'distribution'
 *      url: // value for 'url'
 *      accessToken: // value for 'accessToken'
 *      testConsumer: // value for 'testConsumer'
 *      uploadState: // value for 'uploadState'
 *      autoUpdate: // value for 'autoUpdate'
 *   },
 * });
 */
export function useChangeProviderMutation(baseOptions?: Apollo.MutationHookOptions<ChangeProviderMutation, ChangeProviderMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<ChangeProviderMutation, ChangeProviderMutationVariables>(ChangeProviderDocument, options);
      }
export type ChangeProviderMutationHookResult = ReturnType<typeof useChangeProviderMutation>;
export type ChangeProviderMutationResult = Apollo.MutationResult<ChangeProviderMutation>;
export type ChangeProviderMutationOptions = Apollo.BaseMutationOptions<ChangeProviderMutation, ChangeProviderMutationVariables>;
export const RemoveProviderDocument = gql`
    mutation removeProvider($distribution: String!) {
  removeProvider(distribution: $distribution)
}
    `;
export type RemoveProviderMutationFn = Apollo.MutationFunction<RemoveProviderMutation, RemoveProviderMutationVariables>;

/**
 * __useRemoveProviderMutation__
 *
 * To run a mutation, you first call `useRemoveProviderMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useRemoveProviderMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [removeProviderMutation, { data, loading, error }] = useRemoveProviderMutation({
 *   variables: {
 *      distribution: // value for 'distribution'
 *   },
 * });
 */
export function useRemoveProviderMutation(baseOptions?: Apollo.MutationHookOptions<RemoveProviderMutation, RemoveProviderMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<RemoveProviderMutation, RemoveProviderMutationVariables>(RemoveProviderDocument, options);
      }
export type RemoveProviderMutationHookResult = ReturnType<typeof useRemoveProviderMutation>;
export type RemoveProviderMutationResult = Apollo.MutationResult<RemoveProviderMutation>;
export type RemoveProviderMutationOptions = Apollo.BaseMutationOptions<RemoveProviderMutation, RemoveProviderMutationVariables>;
export const SubscribeLogsDocument = gql`
    subscription subscribeLogs($service: String, $instance: String, $process: String, $directory: String, $task: String, $prefetch: Int!, $levels: [String!]) {
  subscribeLogs(
    service: $service
    instance: $instance
    process: $process
    directory: $directory
    task: $task
    prefetch: $prefetch
    levels: $levels
  ) {
    sequence
    payload {
      time
      level
      unit
      message
      terminationStatus
    }
  }
}
    `;

/**
 * __useSubscribeLogsSubscription__
 *
 * To run a query within a React component, call `useSubscribeLogsSubscription` and pass it any options that fit your needs.
 * When your component renders, `useSubscribeLogsSubscription` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the subscription, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useSubscribeLogsSubscription({
 *   variables: {
 *      service: // value for 'service'
 *      instance: // value for 'instance'
 *      process: // value for 'process'
 *      directory: // value for 'directory'
 *      task: // value for 'task'
 *      prefetch: // value for 'prefetch'
 *      levels: // value for 'levels'
 *   },
 * });
 */
export function useSubscribeLogsSubscription(baseOptions: Apollo.SubscriptionHookOptions<SubscribeLogsSubscription, SubscribeLogsSubscriptionVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useSubscription<SubscribeLogsSubscription, SubscribeLogsSubscriptionVariables>(SubscribeLogsDocument, options);
      }
export type SubscribeLogsSubscriptionHookResult = ReturnType<typeof useSubscribeLogsSubscription>;
export type SubscribeLogsSubscriptionResult = Apollo.SubscriptionResult<SubscribeLogsSubscription>;
export const TaskTypesDocument = gql`
    query taskTypes {
  taskTypes
}
    `;

/**
 * __useTaskTypesQuery__
 *
 * To run a query within a React component, call `useTaskTypesQuery` and pass it any options that fit your needs.
 * When your component renders, `useTaskTypesQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useTaskTypesQuery({
 *   variables: {
 *   },
 * });
 */
export function useTaskTypesQuery(baseOptions?: Apollo.QueryHookOptions<TaskTypesQuery, TaskTypesQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<TaskTypesQuery, TaskTypesQueryVariables>(TaskTypesDocument, options);
      }
export function useTaskTypesLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<TaskTypesQuery, TaskTypesQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<TaskTypesQuery, TaskTypesQueryVariables>(TaskTypesDocument, options);
        }
export type TaskTypesQueryHookResult = ReturnType<typeof useTaskTypesQuery>;
export type TaskTypesLazyQueryHookResult = ReturnType<typeof useTaskTypesLazyQuery>;
export type TaskTypesQueryResult = Apollo.QueryResult<TaskTypesQuery, TaskTypesQueryVariables>;
export const TasksDocument = gql`
    query tasks($task: String, $type: String, $parameters: [TaskParameterInput!], $onlyActive: Boolean, $limit: Int) {
  tasks(
    task: $task
    type: $type
    parameters: $parameters
    onlyActive: $onlyActive
    limit: $limit
  ) {
    task
    type
    parameters {
      name
      value
    }
    creationTime
    active
  }
}
    `;

/**
 * __useTasksQuery__
 *
 * To run a query within a React component, call `useTasksQuery` and pass it any options that fit your needs.
 * When your component renders, `useTasksQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useTasksQuery({
 *   variables: {
 *      task: // value for 'task'
 *      type: // value for 'type'
 *      parameters: // value for 'parameters'
 *      onlyActive: // value for 'onlyActive'
 *      limit: // value for 'limit'
 *   },
 * });
 */
export function useTasksQuery(baseOptions?: Apollo.QueryHookOptions<TasksQuery, TasksQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<TasksQuery, TasksQueryVariables>(TasksDocument, options);
      }
export function useTasksLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<TasksQuery, TasksQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<TasksQuery, TasksQueryVariables>(TasksDocument, options);
        }
export type TasksQueryHookResult = ReturnType<typeof useTasksQuery>;
export type TasksLazyQueryHookResult = ReturnType<typeof useTasksLazyQuery>;
export type TasksQueryResult = Apollo.QueryResult<TasksQuery, TasksQueryVariables>;
export const CancelTaskDocument = gql`
    mutation cancelTask($task: String!) {
  cancelTask(task: $task)
}
    `;
export type CancelTaskMutationFn = Apollo.MutationFunction<CancelTaskMutation, CancelTaskMutationVariables>;

/**
 * __useCancelTaskMutation__
 *
 * To run a mutation, you first call `useCancelTaskMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useCancelTaskMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [cancelTaskMutation, { data, loading, error }] = useCancelTaskMutation({
 *   variables: {
 *      task: // value for 'task'
 *   },
 * });
 */
export function useCancelTaskMutation(baseOptions?: Apollo.MutationHookOptions<CancelTaskMutation, CancelTaskMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<CancelTaskMutation, CancelTaskMutationVariables>(CancelTaskDocument, options);
      }
export type CancelTaskMutationHookResult = ReturnType<typeof useCancelTaskMutation>;
export type CancelTaskMutationResult = Apollo.MutationResult<CancelTaskMutation>;
export type CancelTaskMutationOptions = Apollo.BaseMutationOptions<CancelTaskMutation, CancelTaskMutationVariables>;