package io.github.oybek.database

import io.github.oybek.database.*
import io.github.oybek.database.organizer.*
import io.github.oybek.database.admin.dao.AdminDaoSpec
import io.github.oybek.database.organizer.dao.OrganizerDaoSpec

class DaoSpec extends MigrationSpec
              with AdminDaoSpec
              with BalanceDaoSpec
              with OrganizerDaoSpec
              with TransactionSpec
