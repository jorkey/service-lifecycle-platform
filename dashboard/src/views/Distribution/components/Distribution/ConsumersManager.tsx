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
import Grid, {EditColumnParams} from "../../../../common/EditTable";
import {
  useAddConsumerMutation, useChangeConsumerMutation,
  useConsumersInfoQuery, useRemoveConsumerMutation,
  useServiceProfilesQuery
} from "../../../../generated/graphql";
import ConfirmDialog from "../../../../common/ConfirmDialog";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  consumersTable: {
    marginTop: 20
  },
  distributionColumn: {
    padding: '4px',
    paddingLeft: '16px',
    width: '400px'
  },
  serviceColumn: {
    padding: '4px',
    paddingLeft: '16px',
    width: '400px'
  },
  testConsumerColumn: {
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

  const { data: consumers, refetch: refetchConsumers } = useConsumersInfoQuery({ fetchPolicy: 'no-cache' })
  const { data: serviceProfiles } = useServiceProfilesQuery()

  const [ addConsumer ] = useAddConsumerMutation({
    onError(err) { console.log(err) }
  })
  const [ changeConsumer ] = useChangeConsumerMutation({
    onError(err) { console.log(err) }
  })
  const [ removeConsumer ] = useRemoveConsumerMutation({
    onError(err) { console.log(err) }
  })


  const [ addNewRow, setAddNewRow ] = useState(false)
  const [ deleteConfirm, setDeleteConfirm ] = useState<string>()

  const columns: Array<EditColumnParams> = [
    {
      name: 'distribution',
      headerName: 'Distribution',
      className: classes.distributionColumn,
      editable: true,
      validate: (value, rowNum) => {
        return !!value &&
          !rows.find((row, index) => {
            return index != rowNum && row.get('distribution') == value
          })
      }
    },
    {
      name: 'profile',
      headerName: 'Profile',
      className: classes.serviceColumn,
      editable: !!serviceProfiles,
      select: serviceProfiles?.serviceProfiles.map(profile => profile.profile),
      validate: (value) => !!value
    },
    {
      name: 'testConsumer',
      headerName: 'Test Consumer',
      className: classes.testConsumerColumn,
      editable: true,
      validate: () => true
    }
  ]

  const rows = new Array<Map<string, string>>()
  consumers?.consumersInfo.forEach(consumer => { rows.push(new Map([
    ['distribution', consumer.distribution],
    ['profile', consumer.profile],
    ['testConsumer', consumer.testConsumer?consumer.testConsumer:'']
  ])) })

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
              onClick={() => setAddNewRow(true) }
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
          <Grid
            className={classes.consumersTable}
            columns={columns}
            rows={rows}
            addNewRow={addNewRow}
            onRowAddCancelled={
              () => setAddNewRow(false)
            }
            onRowAdded={(row) => {
              setAddNewRow(false)
              addConsumer({ variables: {
                distribution: row.get('distribution')! as string,
                profile: row.get('profile')! as string,
                testConsumer: row.get('testConsumer') as string
              }}).then(() => refetchConsumers())
            }}
            onRowChange={(row, oldValues, newValues) => {
              setAddNewRow(false)
              changeConsumer({ variables: {
                  distribution: newValues.get('distribution')! as string,
                  profile: newValues.get('profile')! as string,
                  testConsumer: newValues.get('testConsumer') as string
                }}).then(() => refetchConsumers())
            }}
            onRowRemove={(row, values) =>
              setDeleteConfirm(values.get('distribution')! as string)
            }
          />
          { deleteConfirm ? (
            <ConfirmDialog
              message={`Do you want to delete consumer '${deleteConfirm}'?`}
              open={true}
              close={() => { setDeleteConfirm(undefined) }}
              onConfirm={() => removeConsumer({ variables: {
                  distribution: deleteConfirm }}).then(() => refetchConsumers()) }
            />) : null }
        </div>
      </CardContent>
    </Card>
  );
}

export default ConsumersManager