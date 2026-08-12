//
// Created by saki on 15/11/09.
//

/**
 * \brief Pipeline that stores frames in SQLite database.
 *
 * Persists frames to SQLite for later retrieval.
 *
 * Exports:
 *     SQLiteBufferedPipeline: SQLite storage pipeline.
 *
 * Dependencies:
 *     - libUVCCamera.h: UVC frame types.
 *     - IPipeline.h: Base pipeline.
 *     - sqlite3pp.h: SQLite C++ wrapper.
 *
 * Architecture Note:
 *     Used for recording frames to disk with time-based purging.
 */

#ifndef PUPILMOBILE_SQLITEBUFFEREDPIPELINE_H
#define PUPILMOBILE_SQLITEBUFFEREDPIPELINE_H

#include <stdlib.h>
#include <pthread.h>
#include <list>
#include <string>
#include "Mutex.h"
#include "Condition.h"

#include "libUVCCamera.h"
#include "IPipeline.h"
#include "sqlite3pp.h"

#pragma interface

using namespace android;

#define DTIME_LIMIT_NSEC 30000000000LL		// 30sec

/**
 * \brief Stores frames in SQLite database with purge support.
 *
 * Runs handler thread to insert frames and purge old records.
 *
 * Lifecycle:
 *     Construction → start → running → stop → release
 *
 * Thread Safety:
 *     Handler thread accesses DB. Mutex protects statements.
 *
 * Properties:
 *     db: SQLite database handle.
 *     DTIME_LIMIT_NSEC: Max age for records.
 */
class SQLiteBufferedPipeline : virtual public IPipeline {
private:
	sqlite3pp::database *db;
	// precompile statements
	sqlite3pp::command *sql_insert_one;
	sqlite3pp::query *sql_query_oldest_10;
	sqlite3pp::command *sql_delete_one;
	sqlite3pp::command *sql_delete_older;
	sqlite3pp::query *sql_count;

	pthread_t handler_thread;
	mutable Mutex handler_mutex;
	Condition handler_sync;
	static void *handler_thread_func(void *vptr_args);
	void do_loop();
protected:
	/**
	 * \brief Get number of records in database.
	 *
	 * \return Record count.
	 */
	int getCount();
	/**
	 * \brief Delete records older than specified duration.
	 *
	 * \param[in] dtime Age threshold in nanoseconds.
	 *
	 * \return 0 on success.
	 *
	 * \pre dtime > 0
	 * \post Old records removed.
	 */
	int delete_older(const nsecs_t &dtime);
	/**
	 * \brief Purge old records using default limit.
	 *
	 * \param[in] limit_rel_nsec Age limit.
	 *
	 * \return 0 on success.
	 */
	int purge_older(const nsecs_t &limit_rel_nsec = DTIME_LIMIT_NSEC);
public:
	/**
	 * \brief Construct SQLite pipeline.
	 *
	 * \param[in] database_name Path to SQLite database.
	 * \param[in] clear Whether to clear existing database.
	 *
	 * \pre database_name must be valid.
	 * \post DB initialized.
	 */
	SQLiteBufferedPipeline(const char *database_name, const bool &clear = false);
	/**
	 * \brief Destroy SQLite pipeline.
	 *
	 * Side Effects:
	 *     - Closes database.
	 */
	virtual ~SQLiteBufferedPipeline();
	/**
	 * \brief Release resources.
	 *
	 * \return 0 on success.
	 */
	virtual int release();
	/**
	 * \brief Start handler thread.
	 *
	 * \return 0 on success.
	 */
	virtual int start();
	/**
	 * \brief Stop handler thread.
	 *
	 * \return 0 on success.
	 */
	virtual int stop();
	/**
	 * \brief Queue frame for storage.
	 *
	 * \param[in] frame Frame to store.
	 *
	 * \return 0 on success.
	 *
	 * \pre Pipeline running.
	 */
	virtual int queueFrame(uvc_frame_t *frame);
	/**
	 * \brief Clear all records from database.
	 *
	 * Side Effects:
	 *     - Deletes all rows.
	 */
	virtual void clear();
};


#endif //PUPILMOBILE_SQLITEBUFFEREDPIPELINE_H
