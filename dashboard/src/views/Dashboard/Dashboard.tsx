import React from 'react';
import Versions from "./components/Versions/Versions";
import Builds from "./components/Builds/Builds";

interface DashboardProps {
}

const Dashboard: React.FC<DashboardProps> = props => {
  return (
    <div>
      <Builds/>
      <Versions/>
    </div>
  );
};

export default Dashboard;
