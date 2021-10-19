import React, {useEffect, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Grid, Select, Table, TableBody, TableCell, TableRow,
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
  infoCellHeader: {
    width: "100px",
    paddingTop: "2px",
    paddingBottom: "2px"
  },
  infoCellValue: {
    paddingTop: "2px",
    paddingBottom: "2px"
  },
}));

interface FaultInfoCardParams {
  report: DistributionFaultReport
}

const FaultInfoCard: React.FC<FaultInfoCardParams> = props => {
  const { report } = props

  const classes = useStyles()

  return <Card>
    <CardHeader title={'Fault Info'}/>
    <CardContent className={classes.content}>
      <div className={classes.inner}>
        <Table>
          <TableBody>
            <TableRow>
              <TableCell className={classes.infoCellHeader}>Fault Id</TableCell>
              <TableCell className={classes.infoCellValue}>{report.payload.id}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell className={classes.infoCellHeader}>Instance</TableCell>
              <TableCell className={classes.infoCellValue}>{report.payload.info.instance}</TableCell>
            </TableRow>
            {report.payload.info.state.installTime?
              <TableRow>
                <TableCell className={classes.infoCellHeader}>Install Time</TableCell>
                <TableCell className={classes.infoCellValue}>{report.payload.info.state.installTime.toString()}</TableCell>
              </TableRow>:null}
            {report.payload.info.state.lastExitCode?
              <TableRow>
                <TableCell className={classes.infoCellHeader}>Last Exit Code</TableCell>
                <TableCell className={classes.infoCellValue}>{report.payload.info.state.lastExitCode}</TableCell>
              </TableRow>:null}
            {report.payload.info.state.failuresCount?
              <TableRow>
                <TableCell className={classes.infoCellHeader}>Failures Count</TableCell>
                <TableCell className={classes.infoCellValue}>{report.payload.info.state.failuresCount}</TableCell>
              </TableRow>:null}
            {report.payload.info.state.updateToVersion?
              <TableRow>
                <TableCell className={classes.infoCellHeader}>Update To Version</TableCell>
                <TableCell className={classes.infoCellValue}>{Version.clientDistributionVersionToString(report.payload.info.state.updateToVersion)}</TableCell>
              </TableRow>:null}
            {report.payload.info.state.updateError?
              <TableRow>
                <TableCell className={classes.infoCellHeader}>Update Error</TableCell>
                <TableCell className={classes.infoCellValue}>{report.payload.info.state.updateError}</TableCell>
              </TableRow>:null}
          </TableBody>
        </Table>
      </div>
    </CardContent>
  </Card>
}

export default FaultInfoCard