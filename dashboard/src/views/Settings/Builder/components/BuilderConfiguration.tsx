import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Divider, Box, Link,
} from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import {Redirect, useRouteMatch} from "react-router-dom";
import { NavLink as RouterLink } from 'react-router-dom';
import GridTable from "../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";
import DeleteIcon from "@material-ui/icons/Delete";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  servicesTable: {
  },
  serviceColumn: {
    width: '200px',
  },
  actionsColumn: {
    width: '200px',
    paddingRight: '40px',
    textAlign: 'right'
  },
  alert: {
    marginTop: 25
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

interface ServicesManagerParams {
  title: string
  services: string[]
  removeServiceConfig: (service: string) => void
  error?: string
}

const BuilderConfiguration: React.FC<ServicesManagerParams> = props => {
  const { title, services, removeServiceConfig, error } = props

  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

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

  const rows = new Array<Map<string, GridTableCellParams>>()
  services
    .sort((s1, s2) =>  (s1 > s2 ? 1 : -1))
    .forEach(service => {
      rows.push(
        new Map<string, GridTableCellParams>([
          ['service', { value: service }],
          ['actions',
            { value: [<Button key='0' onClick={ () => setDeleteConfirm(service) }>
                <DeleteIcon/>
              </Button>] }]
        ]))
    })

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
              Add New
            </Button>
          </Box>
        }
        title={title}
      />
      <Divider/>
      <CardContent className={classes.content}>
        <GridTable
          className={classes.servicesTable}
          columns={columns}
          rows={rows}
          onClick={ (row) =>
            setStartEdit(rows[row].get('service')?.value! as string)
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
              removeServiceConfig(deleteConfirm)
              setDeleteConfirm('')
            }}
          />) : null
        }
      </CardContent>
    </Card>
  );
}

export default BuilderConfiguration