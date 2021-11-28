import {Box, Button, Card, CardContent, CardHeader, FormControlLabel} from "@material-ui/core";
import FormGroup from "@material-ui/core/FormGroup";
import TimeSelector from "./TimeSelector";
import {RefreshControl} from "../../common/components/refreshControl/RefreshControl";
import GridTable from "../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import React from "react";
import {GridTableCellParams, GridTableColumnParams} from "../../common/components/gridTable/GridTableColumn";

export class VersionWrap<Version> {
  original: Version
  service: string
  version: string
  author: string | undefined
  buildTime: string | undefined
  comment: string | undefined

  constructor(desiredVersion: Version,
              service: string, version: string,
              author: string | undefined,
              buildTime: string | undefined,
              comment: string | undefined) {
    this.original = desiredVersion
    this.service = service
    this.version = version
    this.author = author
    this.buildTime = buildTime
    this.comment = comment
  }
}

type Classes = Record<"root" | "content" | "inner" | "versionsTable" |
                      "serviceColumn" | "versionColumn" | "boldVersionColumn" | "authorColumn" | "timeColumn" | "commentColumn" |
                      "controls" | "control" | "alert" | "historyButton", string>

export class DesiredVersionsView<Version> {
  private title: string
  private wrap: (version: Version) => VersionWrap<Version>
  private compare: (v1: VersionWrap<Version> | undefined,
                    v2: VersionWrap<Version> | undefined) => number
  private modifyDesiredVersions: (desiredVersions: Version[]) => void
  private rerender: () => void
  private refresh: () => void
  private classes: Classes

  private desiredVersions: VersionWrap<Version>[] | undefined
  private originalDesiredVersions: VersionWrap<Version>[] | undefined
  private desiredVersionsHistory: {time:Date, versions:VersionWrap<Version>[]}[] | undefined
  private versionsHistory: VersionWrap<Version>[] | undefined

  private columns: Array<GridTableColumnParams> = []
  private timeSelect: Date | undefined = undefined
  private error: string | undefined = undefined

  constructor(title: string,
              wrap: (version: Version) => VersionWrap<Version>,
              compare: (v1: VersionWrap<Version> | undefined, v2: VersionWrap<Version> | undefined) => number,
              modifyDesiredVersions: (desiredVersions: Version[]) => void,
              rerender: () => void, refresh: () => void, classes: Classes) {
    this.title = title
    this.wrap = wrap
    this.compare = compare
    this.modifyDesiredVersions = modifyDesiredVersions
    this.rerender = rerender
    this.refresh = refresh
    this.classes = classes
    this.columns = this.getBaseColumns()
  }

  getBaseColumns() {
    return [
      {
        name: 'service',
        headerName: 'Service',
        className: this.classes.serviceColumn,
      },
      {
        name: 'version',
        type: 'select',
        headerName: 'Desired Version',
        className: this.classes.versionColumn,
        editable: true
      },
      {
        name: 'author',
        headerName: 'Author',
        className: this.classes.authorColumn,
      },
      {
        name: 'buildTime',
        headerName: 'Build Time',
        type: 'date',
        className: this.classes.timeColumn,
      },
      {
        name: 'comment',
        headerName: 'Comment',
        className: this.classes.commentColumn,
      }
    ] as Array<GridTableColumnParams>
  }

  setColumns(columns: Array<GridTableColumnParams>) {
    this.columns = columns
  }

  setDesiredVersions(desiredVersions: Version[]) {
    this.desiredVersions = desiredVersions.map(version => this.wrap(version))
    this.originalDesiredVersions = [...this.desiredVersions]
    this.rerender()
  }

  setDesiredVersionsHistory(desiredVersionsHistory: {time:Date, versions:Version[]}[]) {
    this.desiredVersionsHistory =
      desiredVersionsHistory.map(v => { return {time: v.time, versions: v.versions.map(v => this.wrap(v))}})
    this.rerender()
  }

  setVersionsHistory(versionsHistory: Version[]) {
    console.log('setVersionsHistory')
    this.versionsHistory = versionsHistory.map(v => this.wrap(v))
    this.rerender()
  }

  setError(error: string | undefined) {
    this.error = error
    this.rerender()
  }

  isDataReady() {
    return !!this.desiredVersions && !!this.desiredVersionsHistory && !!this.versionsHistory
  }

  makeBaseRows() {
    return this.makeServicesList().map(service => {
      const l = this.versionsHistory!.filter(v => v.service == service)?.map(v => v.version)
      console.log('versions for ' + service + ' ' + l.length)

      const currentVersion = this.desiredVersions!.find(v => v.service == service)
      const originalVersion = this.originalDesiredVersions!.find(v => v.service == service)
      const version = currentVersion ? currentVersion : originalVersion!
      const modified = this.compare(currentVersion, originalVersion)
      return new Map<string, GridTableCellParams>([
        ['service', { value: service }],
        ['version', {
          value: version.version,
          className: modified?this.classes.boldVersionColumn:undefined,
          select: this.versionsHistory!.filter(v => v.service == service)?.map(v => v.version)
        }],
        ['author', { value: version.author }],
        ['buildTime', { value: version.buildTime }],
        ['comment', { value: version.comment }]
      ])})
  }

  private makeServicesList() {
    const services = new Set<string>()
    this.originalDesiredVersions!.map(v => v.service).forEach(s => services.add(s))
    this.desiredVersions!.map(v => v.service).forEach(s => services.add(s))
    return Array.from(services)
  }

  render(rows: Map<string, GridTableCellParams>[]) {
    return (
      <Card
        className={this.classes.root}
      >
        <CardHeader
          action={
            <FormGroup row>
              <FormControlLabel
                label={null}
                control={
                  this.timeSelect ?
                    <TimeSelector time={this.timeSelect}
                                  times={this.desiredVersionsHistory!.map(v => v.time).sort()}
                                  onSelected={(t) => {
                                    this.timeSelect = t
                                    const versions = this.desiredVersionsHistory!.find(v => v.time == this.timeSelect)?.versions
                                    if (versions) {
                                      this.modifyDesiredVersions(versions.map(v => v.original))
                                    }
                                  }}
                    /> :
                    <Button
                      className={this.classes.historyButton}
                      color="primary"
                      variant="contained"
                      onClick={() => {
                        const history = this.desiredVersionsHistory!.map(v => v.time).sort()
                        if (history.length) {
                          this.timeSelect = history[history.length-1]
                          this.rerender()
                        }
                      }}
                    >
                      History
                    </Button>
                }/>
              <RefreshControl className={this.classes.control}
                              refresh={() => { this.refresh() }}
              />
            </FormGroup>
          }
          title={this.title}
        />
        <CardContent className={this.classes.content}>
          <div className={this.classes.inner}>
            <GridTable
              className={this.classes.versionsTable}
              columns={this.columns}
              rows={rows}
              onRowChanged={ () => {
                console.log('onRowChanged')
              } }
            />
            {this.error && <Alert className={this.classes.alert} severity="error">{this.error}</Alert>}
            {this.timeSelect ?
              <Box className={this.classes.controls}>
                <Button className={this.classes.control}
                        color="primary"
                        variant="contained"
                        onClick={() => { this.timeSelect = undefined; this.rerender() }}
                >
                  Cancel
                </Button>
                <Button className={this.classes.control}
                        color="primary"
                        variant="contained"
                        onClick={() => { this.timeSelect = undefined; this.rerender() }}
                >
                  Save
                </Button>
              </Box> : null}
          </div>
        </CardContent>
      </Card>
    )
  }
}
