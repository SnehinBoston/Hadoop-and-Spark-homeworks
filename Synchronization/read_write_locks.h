#include <unistd.h>
#include <stdio.h>
#include <time.h>
#include <assert.h>
#include <pthread.h>

#define SLEEP_MS 1000000 /* 1 millisecond == 1 million nsec. */
#define NUM_TASKS 10
#define NUM_READERS 2
#define NUM_WRITERS 2

extern void *reader(void *);
extern void begin_read(void);
extern void end_read(void);
extern void *writer(void *);
extern void begin_write(void);
extern void end_write(void);

extern void acquire_shared_lock(void);
extern void release_shared_lock(void);
extern void acquire_exclusive_lock(void);
extern void release_exclusive_lock(void);
