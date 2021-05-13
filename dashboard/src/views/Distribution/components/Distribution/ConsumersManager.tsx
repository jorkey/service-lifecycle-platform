import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Divider, Box,
} from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import { useRouteMatch} from "react-router-dom";
import { NavLink as RouterLink } from 'react-router-dom';
import EditTable, {EditColumnParams} from "../../../../common/EditTable";
import {useServiceProfilesQuery} from "../../../../generated/graphql";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  distributionColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  profileColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  testDistributionMatchColumn: {
    padding: '4px',
    paddingLeft: '16px'
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

const ConsumersManager = () => {
  const classes = useStyles()
  const routeMatch = useRouteMatch();

  const { data: serviceProfiles } = useServiceProfilesQuery()

  const columns: Array<EditColumnParams> = [
    {
      name: 'distribution',
      headerName: 'Distribution',
      className: classes.distributionColumn,
      editable: true,
      validate: (value) => !!value
    },
    {
      name: 'profile',
      headerName: 'Profile',
      className: classes.profileColumn,
      editable: !!serviceProfiles,
      select: serviceProfiles?.serviceProfiles.map(profile => profile.profile),
      validate: (value) => !!value
    },
    {
      name: 'testDistributionMatch',
      headerName: 'Test Distribution Match',
      className: classes.testDistributionMatchColumn,
      editable: true,
      validate: () => true
    }
  ]

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <Box
            className={classes.controls}
          >
            <Button
              color="primary"
              variant="contained"
              className={classes.control}
              startIcon={<AddIcon/>}
              component={RouterLink}
              to={`${routeMatch.url}/new`}
            >
              Add New Consumer
            </Button>
          </Box>
        }
        title={'Consumers'}
      />
      <Divider/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <EditTable columns={columns}
            userType={userType}/>
        </div>
      </CardContent>
    </Card>
  );
}

export default ConsumersManager