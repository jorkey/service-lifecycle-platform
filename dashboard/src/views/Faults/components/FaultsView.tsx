import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Grid
} from '@material-ui/core';
import {RouteComponentProps} from "react-router-dom";
import FaultInfoCard from "./FaultInfoCard";
import FaultFiles from "./FaultFiles";
import {LogsTailCard} from "./LogsTailCard";
import FaultsCard from "./FaultsCard";
import {DistributionFaultReport} from "../../../generated/graphql";

const useStyles = makeStyles((theme:any) => ({
  root: {
    padding: theme.spacing(2)
  }
}));

interface FaultsRouteParams {
}

interface FaultsParams extends RouteComponentProps<FaultsRouteParams> {
  fromUrl: string
}

const FaultsView: React.FC<FaultsParams> = props => {
  const classes = useStyles()

  const [report, setReport] = useState<DistributionFaultReport>()

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <FaultsCard onSelected={report => setReport(report)}/>
        </Grid>
        {report?<>
          <Grid item xs={4}>
            <Grid container direction={'column'} spacing={3}>
              <Grid item xs={report.payload.files.length?6:12}>
                <FaultInfoCard report={report}/>
              </Grid>
              {report.payload.files.length?
                <Grid item xs={6}>
                  <FaultFiles files={report.payload.files}/>
                </Grid>:null}
            </Grid>
          </Grid>
          <Grid item xs={8}>
            <LogsTailCard lines={report.payload.info.logTail}/>
          </Grid>
        </>:null}
      </Grid>
    </div>
  )
}

export default FaultsView