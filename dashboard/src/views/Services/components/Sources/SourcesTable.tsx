import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import EditTable, {EditColumnParams} from "../../../../common/EditTable";
import ConfirmDialog from "../../../../common/ConfirmDialog";
import {SourceConfig} from "../../../../generated/graphql";

const useStyles = makeStyles(theme => ({
  servicesTable: {
    marginTop: 20
  },
  nameColumn: {
    width: '250px',
    padding: '4px',
    paddingLeft: '16px'
  },
  urlColumn: {
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
  onSourceChange?: (oldSource: SourceConfig, newSource: SourceConfig) => void
  onSourceRemove?: (source: SourceConfig) => void
}

export const SourcesTable = (props: SourceTableParams) => {
  const { sources, addSource, confirmRemove,
    onSourceAdded, onSourceAddCancelled, onSourceChange, onSourceRemove } = props;

  const [ deleteConfirm, setDeleteConfirm ] = useState<SourceConfig>()

  const classes = useStyles();

  const columns: Array<EditColumnParams> = [
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

  const rows = new Array<Map<string, string>>()
  sources.forEach(source => { rows.push(new Map([
    ['name', source.name],
    ['url', source.git.url],
    ['cloneSubmodules', String(source.git.cloneSubmodules)]
  ])) })

  return (<>
    <EditTable
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      addNewRow={addSource}
      onRowAdded={ (columns) => {
        onSourceAdded?.({ name: columns.get('name')! as string,
          git: { url: columns.get('url')! as string, cloneSubmodules: columns.get('cloneSubmodules') as boolean } }) }}
      onRowAddCancelled={onSourceAddCancelled}
      onRowChange={ (row, oldValues, newValues) => {
        console.log('--- cloneSubmodules ' + newValues.get('cloneSubmodules'))
        onSourceChange?.(sources[row], { name: newValues.get('name')! as string,
          git: { url: newValues.get('url')! as string, cloneSubmodules: newValues.get('cloneSubmodules') as boolean } }) }}
      onRowRemove={ (row) => {
        return confirmRemove ? setDeleteConfirm(sources[row]) : onSourceRemove?.(sources[row])
      }}
    />
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete source '${deleteConfirm.name}'?`}
        open={true}
        close={() => { setDeleteConfirm(undefined) }}
        onConfirm={() => {
          onSourceRemove?.(deleteConfirm)
          setDeleteConfirm(undefined)
        }}
      />) : null }
  </>)
}
