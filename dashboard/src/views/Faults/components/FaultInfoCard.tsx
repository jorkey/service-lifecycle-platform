import React, {useEffect, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Grid, Select, Table, TableBody, TableCell, TableRow,
} from '@material-ui/core';
import {
  DistributionFaultReport,
} from "../../../generated/graphql";
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
            <TableRow hover>
              <TableCell className={classes.infoCellHeader}>Fault Id</TableCell>
              <TableCell className={classes.infoCellValue}>{report.fault}</TableCell>
            </TableRow>
            <TableRow hover>
              <TableCell className={classes.infoCellHeader}>Instance</TableCell>
              <TableCell className={classes.infoCellValue}>{report.info.instance}</TableCell>
            </TableRow>
            {report.info.state.installTime?
              <TableRow hover>
                <TableCell className={classes.infoCellHeader}>Install Time</TableCell>
                <TableCell className={classes.infoCellValue}>{report.info.state.installTime.toString()}</TableCell>
              </TableRow>:null}
            {report.info.state.lastExitCode?
              <TableRow hover>
                <TableCell className={classes.infoCellHeader}>Last Exit Code</TableCell>
                <TableCell className={classes.infoCellValue}>{report.info.state.lastExitCode}</TableCell>
              </TableRow>:null}
            {report.info.state.failuresCount?
              <TableRow hover>
                <TableCell className={classes.infoCellHeader}>Failures Count</TableCell>
                <TableCell className={classes.infoCellValue}>{report.info.state.failuresCount}</TableCell>
              </TableRow>:null}
            {report.info.state.updateToVersion?
              <TableRow hover>
                <TableCell className={classes.infoCellHeader}>Update To Version</TableCell>
                <TableCell className={classes.infoCellValue}>{Version.clientDistributionVersionToString(
                  report.info.state.updateToVersion)}</TableCell>
              </TableRow>:null}
            {report.info.state.updateError?
              <TableRow hover>
                <TableCell className={classes.infoCellHeader}>Update Error</TableCell>
                <TableCell className={classes.infoCellValue}>{report.info.state.updateError}</TableCell>
              </TableRow>:null}
          </TableBody>
        </Table>
      </div>
    </CardContent>
  </Card>
}

export default FaultInfoCard