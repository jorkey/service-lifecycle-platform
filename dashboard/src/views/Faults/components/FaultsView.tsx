import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import { Grid } from '@material-ui/core';
import {RouteComponentProps} from "react-router-dom";
import FaultInfoCard from "./FaultInfoCard";
import FaultFiles from "./FaultFiles";
import {LogsTailCard} from "./LogsTailCard";
import FaultsCard from "./FaultsCard";
import {DistributionFaultReport} from "../../../generated/graphql";

interface FaultsRouteParams {
}

interface FaultsParams extends RouteComponentProps<FaultsRouteParams> {
  fromUrl: string
}

const FaultsView: React.FC<FaultsParams> = props => {
  const [report, setReport] = useState<DistributionFaultReport>()

  return (
    <div>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <FaultsCard onSelected={report => setReport(report)}/>
        </Grid>
        {report?<>
          <Grid item xs={4}>
            <Grid container direction={'column'} spacing={3}>
              <Grid item xs={12}>
                <FaultInfoCard report={report}/>
              </Grid>
              {report.payload.files.length?
                <Grid item xs={12}>
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