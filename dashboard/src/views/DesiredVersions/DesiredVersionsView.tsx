import {Box, Button, Card, CardContent, CardHeader, FormControlLabel} from "@material-ui/core";
import FormGroup from "@material-ui/core/FormGroup";
import TimeSelector from "./TimeSelector";
import {RefreshControl} from "../../common/components/refreshControl/RefreshControl";
import GridTable from "../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import React from "react";
import {GridTableCellParams, GridTableColumnParams} from "../../common/components/gridTable/GridTableColumn";

type Classes = Record<"root" | "content" | "inner" | "versionsTable" |
                      "serviceColumn" | "versionColumn" | "boldVersionColumn" | "authorColumn" | "timeColumn" | "commentColumn" |
                      "controls" | "control" | "alert" | "historyButton", string>

export interface ServiceVersion<Version> {
  service: string
  version: Version
}

export interface VersionInfo {
  author: string
  buildTime: string
  comment: string
}

export class DesiredVersionsView<Version> {
  private title: string
  private compare: (v1: Version | undefined, v2: Version | undefined) => number
  private serialize: (version: Version) => string
  private parse: (version: string) => Version
  private modify: (desiredVersions: ServiceVersion<Version>[]) => void
  private rerender: () => void
  private refresh: () => void
  private classes: Classes

  private desiredVersions: ServiceVersion<Version>[] | undefined
  private originalDesiredVersions: ServiceVersion<Version>[] | undefined
  private desiredVersionsHistory: {time:Date, versions:ServiceVersion<Version>[]}[] | undefined
  private versionsInfo: {version:ServiceVersion<Version>, info: VersionInfo}[] | undefined

  private columns: Array<GridTableColumnParams> = []
  private timeSelect: Date | undefined = undefined
  private error: string | undefined = undefined

  constructor(title: string,
              compare: (v1: Version | undefined, v2: Version | undefined) => number,
              serialize: (version: Version) => string,
              parse: (version: string) => Version,
              modify: (desiredVersions: ServiceVersion<Version>[]) => void,
              rerender: () => void, refresh: () => void, classes: Classes) {
    this.title = title
    this.compare = compare
    this.serialize = serialize
    this.parse = parse
    this.modify = modify
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

  setDesiredVersions(desiredVersions: ServiceVersion<Version>[]) {
    this.desiredVersions = desiredVersions
    this.originalDesiredVersions = [...this.desiredVersions]
    this.rerender()
  }

  setDesiredVersionsHistory(desiredVersionsHistory: {time:Date, versions:ServiceVersion<Version>[]}[]) {
    this.desiredVersionsHistory =
      desiredVersionsHistory.map(v => { return {time: v.time, versions: v.versions}})
    this.rerender()
  }

  setVersionsInfo(versionsInfo: {version:ServiceVersion<Version>, info: VersionInfo}[]) {
    this.versionsInfo = versionsInfo
    this.rerender()
  }

  setError(error: string | undefined) {
    this.error = error
    this.rerender()
  }

  isDataReady() {
    return !!this.desiredVersions && !!this.desiredVersionsHistory && !!this.versionsInfo
  }

  makeBaseRows() {
    return this.makeServicesList().map(service => {
      const currentVersion = this.desiredVersions!.find(v => v.service == service)
      const originalVersion = this.originalDesiredVersions!.find(v => v.service == service)
      const version = currentVersion ? currentVersion : originalVersion!
      const modified = this.compare(currentVersion?.version, originalVersion?.version)
      const info = this.versionsInfo!.find(info => info.version.service == service &&
        this.compare(info.version.version, version?.version) == 0)?.info
      return new Map<string, GridTableCellParams>([
        ['service', { value: service }],
        ['version', {
          value: this.serialize(version.version),
          className: modified?this.classes.boldVersionColumn:undefined,
          select: this.versionsInfo!.filter(v => v.version.service == service)
            ?.map(v => this.serialize(v.version.version))
        }],
        ['author', { value: info?.author }],
        ['buildTime', { value: info?.buildTime }],
        ['comment', { value: info?.comment }]
      ])})
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
                                      this.modify(versions)
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
              onRowChanged={ (row, values, oldValues) => {
                const service = values.get('service')
                const version = this.parse(values.get('version') as string)
                this.desiredVersions = this.desiredVersions?.map(v => {
                  if (v.service == service) {
                    return { service: v.service, version: version }
                  } else {
                    return v
                  }
                })
                this.rerender()
              }}
            />
            {this.error && <Alert className={this.classes.alert} severity="error">{this.error}</Alert>}
            {this.timeSelect || this.isModified() ?
              <Box className={this.classes.controls}>
                <Button className={this.classes.control}
                        color="primary"
                        variant="contained"
                        onClick={() => {
                          this.timeSelect = undefined
                          this.desiredVersions = [...this.originalDesiredVersions!]
                          this.rerender()
                        }}
                >
                  Cancel
                </Button>
                <Button className={this.classes.control}
                        color="primary"
                        variant="contained"
                        disabled={!this.isModified()}
                        onClick={() => {
                          this.timeSelect = undefined
                          this.modify(this.desiredVersions!)
                          this.rerender()
                        }}
                >
                  Save
                </Button>
              </Box> : null}
          </div>
        </CardContent>
      </Card>
    )
  }

  private makeServicesList() {
    const services = new Set<string>()
    this.originalDesiredVersions!.map(v => v.service).forEach(s => services.add(s))
    this.desiredVersions!.map(v => v.service).forEach(s => services.add(s))
    return Array.from(services)
  }

  private isModified() {
    return this.desiredVersions?.length != this.originalDesiredVersions?.length ||
           this.desiredVersions?.find(v1 => {
             const v2 = this.originalDesiredVersions?.find(v2 => v2.service == v1.service)
             return this.compare(v1.version, v2?.version) != 0
           })
  }
}
