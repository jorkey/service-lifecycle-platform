import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import VersionsInProcessCard from "./VersionsInProcessCard";
import LastVersionsCard from "./LastVersionsCard";

const useStyles = makeStyles((theme:any) => ({
}));

const BuildDeveloper = () => {
  // const classes = useStyles()

  return (<>
      <VersionsInProcessCard/>
      <LastVersionsCard/>
    </>
  );
}

export default BuildDeveloper