#!/usr/bin/env bash

#

export databaseQueryModelPort=5432
export databaseQueryModelHost=localhost
export databaseQueryModelDatabase=checklists
export databaseQueryModelSchema=query-model
export databaseQueryModelUserId=$DATABASE_USER_USERID
export databaseQueryModelPassword=$DATABASE_USER_PASSWORD

#

export databaseEventSourcingPort=5432
export databaseEventSourcingHost=localhost
export databaseEventSourcingDatabase=checklists
export databaseEventSourcingSchema=query-model
export databaseEventSourcingUserId=$DATABASE_USER_USERID
export databaseEventSourcingPassword=$DATABASE_USER_PASSWORD
