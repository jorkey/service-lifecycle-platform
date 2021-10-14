import React, {useEffect, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Grid, Select, Table, TableBody, TableCell, TableHead, TableRow,
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
  lengthCell: {
    // width: "50px",
    paddingTop: "2px",
    paddingBottom: "2px"
  },
  inner: {
    // minWidth: 800
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
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Path</TableCell>
              <TableCell className={classes.lengthCell}>Length</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {files.map((file, row) =>
              <TableRow key={row}>
                <TableCell>{file.path}</TableCell>
                <TableCell className={classes.lengthCell}>{file.length}</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </CardContent>
  </Card>
}

export default FaultFiles