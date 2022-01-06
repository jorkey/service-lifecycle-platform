import React from 'react';
import Versions from "./components/Versions/Versions";
import LastClientVersions from "./components/LastClientVersions/LastClientVersions";
import LastDeveloperVersions from "./components/LastDeveloperVersions/LastDeveloperVersions";
import {useBuildDeveloperServicesQuery} from "../../generated/graphql";

interface DashboardProps {
}

const Dashboard: React.FC<DashboardProps> = props => {
  const { data: developerServices } = useBuildDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const development = !!developerServices?.buildDeveloperServicesConfig?.length

  return (
    <div>
      <Versions/>
      {development?<LastDeveloperVersions/>:null}
      <LastClientVersions/>
    </div>
  );
};

export default Dashboard;
