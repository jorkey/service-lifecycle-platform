import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/components/gridTable/GridTable";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import DeleteIcon from "@material-ui/icons/Delete";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";
import {Repository} from "../../../../generated/graphql";

const useStyles = makeStyles(theme => ({
  repositoriesTable: {
    marginTop: 20
  },
  nameColumn: {
    width: '200px',
  },
  urlColumn: {
  },
  branchColumn: {
    width: '200px',
  },
  cloneSubmodulesColumn: {
    width: '150px',
  },
  actionsColumn: {
    width: '200px',
    paddingRight: '40px',
    textAlign: 'right'
  },
}));

interface RepositoryTableParams {
  repositories: Array<Repository>
  addRepository?: boolean
  confirmRemove?: boolean
  onRepositoryAdded?: (repository: Repository) => void
  onRepositoryAddCancelled?: () => void
  onRepositoryChanged?: (oldRepository: Repository, newRepository: Repository) => void
  onRepositoryRemoved?: (repository: Repository) => void
}

const RepositoriesTable = (props: RepositoryTableParams) => {
  const { repositories, addRepository, confirmRemove,
    onRepositoryAdded, onRepositoryAddCancelled, onRepositoryChanged, onRepositoryRemoved } = props;

  const [ deleteConfirm, setDeleteConfirm ] = useState<Repository>()

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
            return index != rowNum && row.get('name')?.value == value
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
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = repositories.map(repository => (
    new Map<string, GridTableCellParams>([
      ['name', { value: repository.name }],
      ['url', { value: repository.git.url }],
      ['branch', { value: repository.git.branch }],
      ['cloneSubmodules', { value: repository.git.cloneSubmodules?repository.git.cloneSubmodules:false }],
      ['actions', { value: [<Button key='0' onClick={ () => confirmRemove ? setDeleteConfirm(repository) : onRepositoryRemoved?.(repository) }>
        <DeleteIcon/>
      </Button>] }]
    ])))

  return (<>
    <GridTable
      className={classes.repositoriesTable}
      columns={columns}
      rows={rows}
      addNewRow={addRepository}
      onRowAdded={ (columns) => {
        onRepositoryAdded?.({ name: columns.get('name')! as string,
          git: { url: columns.get('url')! as string, branch: columns.get('branch')! as string,
            cloneSubmodules: columns.get('cloneSubmodules') as boolean } }) }}
      onRowAddCancelled={onRepositoryAddCancelled}
      onRowChanged={ (row, values, oldValues) => {
        onRepositoryChanged!(repositories[row], { name: values.get('name')! as string,
          git: { url: values.get('url')! as string, branch: values.get('branch')! as string,
            cloneSubmodules: values.get('cloneSubmodules') as boolean } }) }}
    />
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete repository '${deleteConfirm.name}'?`}
        open={true}
        close={() => { setDeleteConfirm(undefined) }}
        onConfirm={() => {
          onRepositoryRemoved?.(deleteConfirm)
          setDeleteConfirm(undefined)
        }}
      />) : null }
  </>)
}

export default RepositoriesTable;
