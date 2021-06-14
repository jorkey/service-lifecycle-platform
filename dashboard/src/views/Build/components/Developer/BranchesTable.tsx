import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/components/gridTable/GridTable";
import ConfirmDialog from "../../../../common/ConfirmDialog";
import {SourceConfig} from "../../../../generated/graphql";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableRow";
import DeleteIcon from "@material-ui/icons/Delete";

const useStyles = makeStyles(theme => ({
  branchesTable: {
  },
  nameColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  branchColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  }
}));

interface SourceTableParams {
  branches: { name: string, branch: string}[]
  editable: boolean
  onBranchesChanged: (sources: { name: string, branch: string}[]) => void
}

const BranchesTable = (props: SourceTableParams) => {
  const { branches, editable, onBranchesChanged } = props;

  const classes = useStyles();

  const columns: GridTableColumnParams[] = [
    {
      name: 'name',
      headerName: 'Name',
      className: classes.nameColumn
    },
    {
      name: 'branch',
      headerName: 'Branch',
      className: classes.branchColumn,
      editable: editable,
      validate: (value, rowNum) => !!value
    }
  ]

  const rows = branches.map(source => new Map<string, GridTableColumnValue>([
    ['name', source.name],
    ['branch', source.branch]
  ]))

  return (<>
    <GridTable
      className={classes.branchesTable}
      columns={columns}
      rows={rows}
      editable={true}
      onRowChanged={ (row, values, oldValues) => {
        onBranchesChanged(branches.map(branch =>
          (branch.name == values.get('name')!)?{
            name: values.get('name')! as string, branch: values.get('branch')! as string}:branch)) }}
    />
  </>)
}

export default BranchesTable;
