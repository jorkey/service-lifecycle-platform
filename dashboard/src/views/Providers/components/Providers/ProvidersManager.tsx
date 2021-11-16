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
  useAddProviderMutation, useChangeProviderMutation,
  useProvidersInfoQuery, useRemoveProviderMutation,
} from "../../../../generated/graphql";
import ConfirmDialog from "../../../../common/ConfirmDialog";
import DeleteIcon from "@material-ui/icons/Delete";
import Alert from "@material-ui/lab/Alert";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableRowParams} from "../../../../common/components/gridTable/GridTableRow";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  providersTable: {
    marginTop: 20
  },
  distributionColumn: {
    padding: '4px',
    paddingLeft: '16px',
    width: '150px'
  },
  urlColumn: {
    padding: '4px',
    paddingLeft: '16px',
  },
  accessTokenColumn: {
    padding: '4px',
    paddingLeft: '16px',
  },
  testConsumerColumn: {
    padding: '4px',
    paddingLeft: '16px',
    width: '400px'
  },
  uploadState: {
    padding: '4px',
    paddingLeft: '16px',
    width: '400px'
  },
  autoUpdate: {
    padding: '4px',
    paddingLeft: '16px',
    width: '400px'
  },
  actionsColumn: {
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right',
    width: '200px',
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

const ProvidersManager = () => {
  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: providers, refetch: refetchProviders } = useProvidersInfoQuery({
    onError(err) { setError('Query providers info error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ addProvider ] = useAddProviderMutation({
    onError(err) { setError('Add provider error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ changeProvider ] = useChangeProviderMutation({
    onError(err) { setError('Change provider error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ removeProvider ] = useRemoveProviderMutation({
    onError(err) { setError('Remove provider error ' + err.message) },
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
            return index != rowNum && row.columnValues.get('distribution') == value
          })
      }
    },
    {
      name: 'url',
      headerName: 'URL',
      className: classes.urlColumn,
      editable: true,
      validate: (value) => {
        try {
          if (!value) {
            return false
          }
          new URL(value as string)
          return true
        } catch {
          return false
        }
      }
    },
    {
      name: 'accessToken',
      headerName: 'Access Token',
      className: classes.accessTokenColumn,
      editable: true
    },
    {
      name: 'testConsumer',
      headerName: 'Test Consumer',
      className: classes.testConsumerColumn,
      editable: true
    },
    {
      name: 'uploadState',
      headerName: 'Upload State',
      className: classes.uploadState,
      type: 'checkbox',
      editable: true,
    },
    {
      name: 'autoUpdate',
      headerName: 'Auto Update',
      className: classes.autoUpdate,
      type: 'checkbox',
      editable: true,
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = new Array<GridTableRowParams>()
  providers?.providersInfo.forEach(provider => { rows.push({
    columnValues: new Map<string, GridTableColumnValue>([
      ['distribution', provider.distribution],
      ['url', provider.url],
      ['accessToken', provider.accessToken],
      ['testConsumer', provider.testConsumer?provider.testConsumer:''],
      ['uploadState', provider.uploadState?true:false],
      ['autoUpdate', provider.autoUpdate?true:false],
      ['actions', [<Button key='0' onClick={ () => setDeleteConfirm(provider.distribution) }>
        <DeleteIcon/>
      </Button>]]
    ])}) })

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
              Add New Provider
            </Button>
          </Box>
        }
        title={'Providers'}
      />
      <Divider/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <GridTable
            className={classes.providersTable}
            columns={columns}
            rows={rows}
            addNewRow={addNewRow}
            onRowAddCancelled={ () => setAddNewRow(false) }
            onRowAdded={(row) => {
              setAddNewRow(false)
              addProvider({ variables: {
                distribution: String(row.get('distribution')!),
                url: String(row.get('url')!),
                accessToken: String(row.get('accessToken')!),
                testConsumer: row.get('testConsumer')?String(row.get('testConsumer')!):undefined,
                uploadState: Boolean(row.get('uploadState')!),
                autoUpdate: Boolean(row.get('autoUpdate')!)
              }}).then(() => refetchProviders())
            }}
            onRowChanged={(row, values, oldValues) => {
              setAddNewRow(false)
              return changeProvider({ variables: {
                  distribution: String(values.get('distribution')!),
                  url: String(values.get('url')!),
                  accessToken: String(values.get('accessToken')!),
                  uploadState: Boolean(values.get('uploadState')!),
                  autoUpdate: Boolean(values.get('autoUpdate')!)
                }}).then(() => refetchProviders().then(() => {}))}}
          />
          { deleteConfirm ? (
            <ConfirmDialog
              message={`Do you want to delete Provider '${deleteConfirm}'?`}
              open={true}
              close={() => { setDeleteConfirm(undefined) }}
              onConfirm={() => removeProvider({ variables: {
                  distribution: deleteConfirm }}).then(() => refetchProviders()) }
            />) : null }
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default ProvidersManager