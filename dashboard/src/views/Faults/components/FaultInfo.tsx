import React, {useEffect, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Grid, Select, TableBody, TableCell, TableRow,
} from '@material-ui/core';
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps} from "react-router-dom";
import {
  DistributionFaultReport,
  useFaultDistributionsQuery, useFaultQuery, useFaultServicesLazyQuery, useFaultsQuery,
  useFaultsStartTimeQuery
} from "../../../generated/graphql";
import {DateTimePicker} from "@material-ui/pickers";
import Alert from "@material-ui/lab/Alert";
import {FaultsTable} from "./FaultsTable";
import TextField from "@material-ui/core/TextField";
import {Version} from "../../../common";

const useStyles = makeStyles((theme:any) => ({
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  infoCell: {
    paddingTop: "2px",
    paddingBottom: "2px"
  },
}));

interface FaultInfoParams {
  report: DistributionFaultReport
}

const FaultInfo: React.FC<FaultInfoParams> = props => {
  const { report } = props

  const classes = useStyles()

  return <Card>
    <CardHeader title={'Fault Info'}/>
    <CardContent className={classes.content}>
      <div className={classes.inner}>
        <TableBody>
          <TableRow>
            <TableCell className={classes.infoCell}>Fault Id</TableCell>
            <TableCell className={classes.infoCell}>{report.payload.id}</TableCell>
          </TableRow>
          <TableRow>
            <TableCell className={classes.infoCell}>Instance</TableCell>
            <TableCell className={classes.infoCell}>{report.payload.info.instance}</TableCell>
          </TableRow>
          {report.payload.info.state.installTime?
            <TableRow>
              <TableCell className={classes.infoCell}>Install Time</TableCell>
              <TableCell className={classes.infoCell}>{report.payload.info.state.installTime.toString()}</TableCell>
            </TableRow>:null}
          {report.payload.info.state.lastExitCode?
            <TableRow>
              <TableCell className={classes.infoCell}>Last Exit Code</TableCell>
              <TableCell className={classes.infoCell}>{report.payload.info.state.lastExitCode}</TableCell>
            </TableRow>:null}
          {report.payload.info.state.failuresCount?
            <TableRow>
              <TableCell className={classes.infoCell}>Failures Count</TableCell>
              <TableCell className={classes.infoCell}>{report.payload.info.state.failuresCount}</TableCell>
            </TableRow>:null}
          {report.payload.info.state.updateToVersion?
            <TableRow>
              <TableCell className={classes.infoCell}>Failures Count</TableCell>
              <TableCell className={classes.infoCell}>{Version.clientDistributionVersionToString(report.payload.info.state.updateToVersion)}</TableCell>
            </TableRow>:null}
          {report.payload.info.state.updateError?
            <TableRow>
              <TableCell className={classes.infoCell}>Update Error</TableCell>
              <TableCell className={classes.infoCell}>{report.payload.info.state.updateError}</TableCell>
            </TableRow>:null}
        </TableBody>
      </div>
    </CardContent>
  </Card>
}

export default FaultInfo