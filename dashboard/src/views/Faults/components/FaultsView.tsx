import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import { Grid } from '@material-ui/core';
import {RouteComponentProps} from "react-router-dom";
import FaultInfoCard from "./FaultInfoCard";
import FaultFiles from "./FaultFiles";
import {LogsTailCard} from "./LogsTailCard";
import FaultsCard from "./FaultsCard";
import {DistributionFaultReport} from "../../../generated/graphql";

const useStyles = makeStyles(theme => ({
  info: {
    display: 'flex'
  }
}));

interface FaultsRouteParams {
}

interface FaultsParams extends RouteComponentProps<FaultsRouteParams> {
}

const FaultsView: React.FC<FaultsParams> = props => {
  const [report, setReport] = useState<DistributionFaultReport>()

  const classes = useStyles()

  return (
    <div>
        <FaultsCard onSelected={report => setReport(report)}/>
        {report?<div className={classes.info}>
          <FaultInfoCard report={report}/>
          {report.files.length?<FaultFiles files={report.files}/>:null}
          <LogsTailCard lines={report.info.logTail}/>
        </div>:null}
    </div>
  )
}

export default FaultsView