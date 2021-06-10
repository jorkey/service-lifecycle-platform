import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/grid/GridTable";
import ConfirmDialog from "../../../../common/ConfirmDialog";
import {SourceConfig} from "../../../../generated/graphql";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/grid/GridTableRow";
import DeleteIcon from "@material-ui/icons/Delete";

const useStyles = makeStyles(theme => ({
  servicesTable: {
    marginTop: 20
  },
  nameColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  urlColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  branchColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  cloneSubmodulesColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  }
}));

interface SourceTableParams {
  sources: Array<SourceConfig>
  addSource?: boolean
  confirmRemove?: boolean
  onSourceAdded?: (source: SourceConfig) => void
  onSourceAddCancelled?: () => void
  onSourceChanged?: (oldSource: SourceConfig, newSource: SourceConfig) => void
  onSourceRemoved?: (source: SourceConfig) => void
}

const SourcesTable = (props: SourceTableParams) => {
  const { sources, addSource, confirmRemove,
    onSourceAdded, onSourceAddCancelled, onSourceChanged, onSourceRemoved } = props;

  const [ deleteConfirm, setDeleteConfirm ] = useState<SourceConfig>()

  const classes = useStyles();

  const columns: GridTableColumnParams[] = [
    {
      name: 'name',
      headerName: 'Name',
      className: classes.nameColumn,
      editable: true,
      validate: (value, rowNum) => {
        return !!value &&
          !rows.find((row, index) => {
            return index != rowNum && row.get('name') == value
          })
      }
    },
    {
      name: 'url',
      headerName: 'URL',
      className: classes.urlColumn,
      editable: true,
      validate: (value, rowNum) => {
        try {
          return value?!!new URL(value as string):false
        } catch (ex) {
          return false
        }
      }
    },
    {
      name: 'branch',
      headerName: 'Branch',
      className: classes.branchColumn,
      editable: true,
      validate: (value, rowNum) => !!value
    },
    {
      name: 'cloneSubmodules',
      headerName: 'Clone Submodules',
      className: classes.cloneSubmodulesColumn,
      type: 'checkbox',
      editable: true,
      validate: (value, rowNum) => {
        return true
      }
    }
  ]

  const rows = sources.map(source => new Map<string, GridTableColumnValue>([
    ['name', source.name],
    ['url', source.git.url],
    ['cloneSubmodules', source.git.cloneSubmodules?source.git.cloneSubmodules:false]
  ]))

  return (<>
    <GridTable
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      editable={true}
      actions={[<DeleteIcon/>]}
      addNewRow={addSource}
      onRowAdded={ (columns) => {
        onSourceAdded?.({ name: columns.get('name')! as string,
          git: { url: columns.get('url')! as string, branch: columns.get('branch')! as string,
            cloneSubmodules: columns.get('cloneSubmodules') as boolean } }) }}
      onRowAddCancelled={onSourceAddCancelled}
      onRowChanged={ (row, values, oldValues) => {
        onSourceChanged!(sources[row], { name: values.get('name')! as string,
          git: { url: values.get('url')! as string, branch: values.get('branch')! as string,
            cloneSubmodules: values.get('cloneSubmodules') as boolean } }) }}
      onAction={ (index, row) => {
        return confirmRemove ? setDeleteConfirm(sources[row]) : onSourceRemoved?.(sources[row])
      }}
    />
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete source '${deleteConfirm.name}'?`}
        open={true}
        close={() => { setDeleteConfirm(undefined) }}
        onConfirm={() => {
          onSourceRemoved?.(deleteConfirm)
          setDeleteConfirm(undefined)
        }}
      />) : null }
  </>)
}

export default SourcesTable;
