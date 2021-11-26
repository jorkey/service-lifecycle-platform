import React, {useState} from 'react';

import {RouteComponentProps, useHistory} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import Alert from "@material-ui/lab/Alert";
import {
  useClientDesiredVersionsHistoryQuery,
  useClientDesiredVersionsQuery, useClientVersionsInfoQuery,
  useDeveloperServicesQuery, useDeveloperVersionsInfoQuery, useSetClientDesiredVersionsMutation,
  useTasksQuery
} from "../../../../generated/graphql";
import {GridTableColumnParams, GridTableCellParams} from "../../../../common/components/gridTable/GridTableColumn";
import {Button, Card, CardContent, CardHeader} from "@material-ui/core";
import VisibilityIcon from "@material-ui/icons/Visibility";
import BuildIcon from "@material-ui/icons/Build";
import {Version} from "../../../../common";
import clsx from "clsx";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import GridTable from "../../../../common/components/gridTable/GridTable";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  versionsTable: {
    marginTop: 20
  },
  serviceColumn: {
    width: '150px',
    padding: '4px',
    paddingLeft: '16px'
  },
  versionColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  authorColumn: {
    width: '150px',
    padding: '4px',
    paddingLeft: '16px'
  },
  commentColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  timeColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '120px',
    padding: '4px',
    paddingRight: '30px',
    textAlign: 'right'
  },
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface DeveloperDesiredVersionsRouteParams {
}

interface DeveloperDesiredVersionsParams extends RouteComponentProps<DeveloperDesiredVersionsRouteParams> {
  fromUrl: string
}

const DeveloperDesiredVersions = (props: DeveloperDesiredVersionsParams) => {
  return null
}

export default DeveloperDesiredVersions;
