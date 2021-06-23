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
import GridTable from "../../../../common/components/gridTable/GridTable";
import {
  useAddConsumerMutation, useChangeConsumerMutation,
  useConsumersInfoQuery, useRemoveConsumerMutation,
  useServiceProfilesQuery
} from "../../../../generated/graphql";
import ConfirmDialog from "../../../../common/ConfirmDialog";
import DeleteIcon from "@material-ui/icons/Delete";
import Alert from "@material-ui/lab/Alert";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";

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
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  },
  controls: {
    display: 'flex',
    justifyContent: 'flex-end'
  },
  control: {
    marginLeft: '50px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

const ConsumersManager = () => {
  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: consumers, refetch: refetchConsumers } = useConsumersInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query consumers info error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const { data: serviceProfiles } = useServiceProfilesQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query service profiles error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ addConsumer ] = useAddConsumerMutation({
    onError(err) { setError('Add consumer error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ changeConsumer, { data: changeConsumerData, error: changeConsumerError } ] = useChangeConsumerMutation({
    onError(err) { setError('Change consumer error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ removeConsumer ] = useRemoveConsumerMutation({
    onError(err) { console.log(err); setError('Remove consumer error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ addNewRow, setAddNewRow ] = useState(false)
  const [ deleteConfirm, setDeleteConfirm ] = useState<string>()

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'distribution',
      headerName: 'Distribution',
      className: classes.distributionColumn,
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
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = new Array<Map<string, GridTableColumnValue>>()
  consumers?.consumersInfo.forEach(consumer => { rows.push(new Map<string, GridTableColumnValue>([
    ['distribution', consumer.distribution],
    ['profile', consumer.profile],
    ['testConsumer', consumer.testConsumer?consumer.testConsumer:''],
    ['actions', [<Button key='0' onClick={ () => setDeleteConfirm(consumer.distribution) }>
      <DeleteIcon/>
    </Button>]]
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
          <GridTable
            className={classes.consumersTable}
            columns={columns}
            rows={rows}
            addNewRow={addNewRow}
            onRowAddCancelled={ () => setAddNewRow(false) }
            onRowAdded={(row) => {
              setAddNewRow(false)
              addConsumer({ variables: {
                distribution: row.get('distribution')! as string,
                profile: row.get('profile')! as string,
                testConsumer: row.get('testConsumer') as string
              }}).then(() => refetchConsumers())
            }}
            onRowChanged={(row, values, oldValues) => {
              setAddNewRow(false)
              return changeConsumer({ variables: {
                  distribution: values.get('distribution')! as string,
                  profile: values.get('profile')! as string,
                  testConsumer: values.get('testConsumer') as string
                }}).then(() => refetchConsumers()).then(() => {})
            }}
          />
          { deleteConfirm ? (
            <ConfirmDialog
              message={`Do you want to delete consumer '${deleteConfirm}'?`}
              open={true}
              close={() => { setDeleteConfirm(undefined) }}
              onConfirm={() => removeConsumer({ variables: {
                  distribution: deleteConfirm }}).then(() => refetchConsumers()) }
            />) : null }
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default ConsumersManager