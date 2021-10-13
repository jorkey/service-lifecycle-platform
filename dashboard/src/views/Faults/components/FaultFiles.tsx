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
  DistributionFaultReport, FileInfo,
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
  infoCell: {
    paddingTop: "2px",
    paddingBottom: "2px"
  },
  inner: {
    minWidth: 800
  },
}));

interface FaultFilesParams {
  files: FileInfo[]
}

const FaultFiles: React.FC<FaultFilesParams> = props => {
  const { files } = props

  const classes = useStyles()

  return <Card>
    <CardHeader title={'Fault Files'}/>
    <CardContent className={classes.content}>
      <div className={classes.inner}>
        <TableBody>
          {files.map(file =>
            <TableRow>
              <TableCell className={classes.infoCell}>{file.path}</TableCell>
              <TableCell className={classes.infoCell}>{file.length}</TableCell>
            </TableRow>
          )}
        </TableBody>
      </div>
    </CardContent>
  </Card>
}

export default FaultFiles