import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent,
} from '@material-ui/core';
import {useRouteMatch} from "react-router-dom";
import {useDeveloperVersionsInfoQuery, useDeveloperVersionsInProcessQuery} from "../../../../generated/graphql";
import Grid, {GridColumnParams} from "../../../../common/Grid";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  controls: {
    display: 'flex',
    justifyContent: 'flex-end'
  },
  control: {
    marginLeft: '50px',
    textTransform: 'none'
  }
}));

const BuildDeveloper = () => {
  const classes = useStyles()
  const routeMatch = useRouteMatch();

  const {data:versionsInProcess} = useDeveloperVersionsInProcessQuery()
  const {data:developerVersionsInfo} = useDeveloperVersionsInfoQuery()

  const columns: Array<GridColumnParams> = [
    {
      name: 'name',
      headerName: 'Name',
      className: classes.nameColumn,
    }]

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <Grid
           className={}
           columns={columns}
           rows={}/>
        </div>
      </CardContent>
    </Card>
  );
}

export default BuildDeveloper