import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useDeveloperServicesQuery, useRemoveServiceSourcesMutation
} from '../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {Redirect, useRouteMatch} from 'react-router-dom';
import ConfirmDialog from '../../../../common/components/dialogs/ConfirmDialog';
import GridTable from "../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";
import {GridTableRowParams} from "../../../../common/components/gridTable/GridTableRow";

const useStyles = makeStyles(theme => ({
  servicesTable: {
  },
  serviceColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  },
  alert: {
    marginTop: 25
  }
}));

const ServicesTable = () => {
  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const [error, setError] = useState<string>()

  const { data: services, refetch } = useDeveloperServicesQuery({
    onError(err) { setError('Query developer services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ removeServiceSources ] = useRemoveServiceSourcesMutation({
    onError(err) { setError('Remove service sources error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const classes = useStyles()

  const routeMatch = useRouteMatch();

  if (startEdit) {
    return <Redirect to={`${routeMatch.url}/edit/${startEdit}`}/>
  }

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = new Array<GridTableRowParams>()
  if (services) {
    [...services.developerServices]
      .sort((s1, s2) =>  (s1 > s2 ? 1 : -1))
      .forEach(service => {
        rows.push(
          {columnValues: new Map<string, GridTableColumnValue>([
            ['service', service],
            ['actions',
              [<Button key='0' onClick={ () => setDeleteConfirm(service) }>
                <DeleteIcon/>
              </Button>]]
          ])})
      })
  }

  return (<>
    <GridTable
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      onClick={ (row) =>
        setStartEdit(rows[row].columnValues.get('service')! as string)
      }
    />
    {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete service '${deleteConfirm}'?`}
        open={true}
        close={() => {
          setDeleteConfirm('')
        }}
        onConfirm={() => {
          removeServiceSources({
            variables: { service: deleteConfirm }
          }).then(() => refetch())
          setDeleteConfirm('')
        }}
      />) : null }
  </>)
}

export default ServicesTable;