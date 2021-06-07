import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useDeveloperServicesQuery, useRemoveServiceSourcesMutation
} from '../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {Redirect, useRouteMatch} from 'react-router-dom';
import ConfirmDialog from '../../../../common/ConfirmDialog';
import {GridTableColumnParams} from "../../../../common/grid/GridTableRow";
import GridTable from "../../../../common/grid/GridTable";

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
  }
}));

const ServicesTable = () => {
  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const { data: services, refetch } = useDeveloperServicesQuery({ fetchPolicy: 'no-cache' })
  const [ removeServiceSources ] = useRemoveServiceSourcesMutation({ onError(err) { console.log(err) }})

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
    }
  ]

  const rows = new Array<Map<string, string>>()
  if (services) {
    [...services.developerServices]
      .sort((s1, s2) =>  (s1 > s2 ? 1 : -1))
      .forEach(service => {
        rows.push(new Map<string, string>([['service', service]]))
      })
  }

  return (<>
    <GridTable
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      actions={[<DeleteIcon/>]}
      onClick={ (row, values) =>
        setStartEdit(values.get('service')! as string) }
      onAction={ (action, row, values) => {
        setDeleteConfirm(values.get('service')! as string)
      }}
    />
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