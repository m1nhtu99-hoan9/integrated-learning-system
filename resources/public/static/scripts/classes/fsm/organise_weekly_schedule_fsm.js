console.debug(
  `'vanillajs-datepicker':` + `\n\tDatepicker imported: ${!!window.Datepicker}`
);
console.debug(`'ramda' imported: ${!!window.R}`);
console.debug(
  `'dayjs' imported: ${!!window.dayjs}.\n\t'isoWeek' plugin imported: ${!!window.dayjs_plugin_isoWeek}.`
);
console.debug(`'xstate' imported: ${!!window.XState}`);

window.dayjs?.extend(window.dayjs_plugin_isoWeek);

function createCurrentMemberTimetableStateService({
  className,
  onTimetablesFetchingStarted,
  onTimetablesFetched,
}) {
  const { assign, interpret, Machine } = window.XState;
  const { log } = window.XState?.actions;

  async function asyncFetchCurrentMemberTimetable(
    { fromDate, toDate },
    classContext
  ) {
    // https://caniuse.com/?search=urlsearchparams
    const uri =
      `/api/v1/classes/${className}/members/timetable?` +
      new URLSearchParams({ "from-date": fromDate, "to-date": toDate });
    let respBody = undefined;
    try {
      const resp = await fetch(uri, {
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
        },
      });
      respBody = await (resp.headers
        .get("content-type")
        .indexOf("application/json") > -1
        ? resp.json()
        : Promise.resolve(undefined));

      if (!resp.ok) {
        console.error(
          `Request for current class members' timetables responded with code ${resp.status}`
        );
        if (!!respBody) {
          // pretty-print the response body
          console.debug(JSON.stringify(respBody, null, 2));
        }
      }
    } catch (e) {
      console.error(
        `Error encountered while fetching from ${uri}. Error: ${e}`
      );
      throw e;
    }

    if (!!respBody?.classTeacher) {
      const { username, displayName, timetable } = respBody.classTeacher;
      classContext.classTeacher[username] = displayName;
      R.forEach(({ timeslotNumber, schoolDate, className }) => {
        const date = dayjs(schoolDate); // parse date
        const weekdayName = weekdayNameFromIndex(date.isoWeekday());

        // preserve queried teacher's timetable entries
        R.when(
          (timetableEntries) =>
            !R.find(
              (x) => x.date === date && x.timeslotNumber === timeslotNumber,
              timetableEntries
            ),
          (timetableEntries) =>
            timetableEntries.push({
              date,
              timeslotNumber,
              weekdayName,
              className,
            })
        )(classContext.timetables.classTeacher);
      }, timetable);
    }
    if (!!respBody?.classStudents) {
      // preserve queried each student's timetable entries
      R.forEach(({ username, displayName, timetable }) => {
        classContext.classStudents[username] = displayName;
        R.forEach(({ timeslotNumber, schoolDate, className }) => {
          const date = dayjs(schoolDate); // parse date
          const weekdayName = weekdayNameFromIndex(date.isoWeekday());

          R.when(
            (timetableEntries) =>
              !R.find(
                (x) =>
                  x.username === username &&
                  x.date === date &&
                  x.timeslotNumber === timeslotNumber,
                timetableEntries
              ),
            (timetableEntries) =>
              timetableEntries.push({
                username,
                date,
                timeslotNumber,
                weekdayName,
                className,
              })
          )(classContext.timetables.classStudents);
        }, timetable);
      })(respBody.classStudents);
    }

    return classContext;
  }

  const guards = {
    dateRangeDefined: (ctx) => !!ctx.fromDate && !!ctx.toDate,
  };
  const delays = {
    brigde: 500, // in miliseconds
  };
  const actions = {
    assignFromDate: assign({
      fromDate: (ctx, eventPayload) => eventPayload?.value ?? ctx.fromDate,
    }),
    assignToDate: assign({
      toDate: (ctx, eventPayload) => eventPayload?.value ?? ctx.toDate,
    }),
  };

  const machine = Machine({
    context: {
      fromDate: undefined,
      toDate: undefined,
      class: {
        classTeacher: {},
        classStudents: {},
        timetables: {
          classTeacher: [],
          classStudents: [],
        },
      },
    },
    initial: "DATE_RANGE_UNDEFINED",
    id: "classCurrentMemberScheduleStateMachine",
    states: {
      DATE_RANGE_UNDEFINED: {
        entry: log("classCurrentMemberScheduleStateMachine initiated"),
        always: {
          target: "MEMBER_TIMETABLES_FETCHING",
          cond: "dateRangeDefined",
        },
        on: {
          DEFINE_FROM_DATE: {
            actions: [
              "assignFromDate",
              (ctx, _) => console.debug(`fromDate: ${ctx.fromDate}`),
            ],
          },
          DEFINE_TO_DATE: {
            actions: [
              "assignToDate",
              (ctx, _) => console.debug(`toDate: ${ctx.toDate}`),
            ],
          },
        },
      },
      MEMBER_TIMETABLES_FETCHING: {
        invoke: {
          src: async (ctx) => await asyncFetchCurrentMemberTimetable(ctx, ctx.class).then(_ => _),  // awaiting deserialising response body 
          id: "fetchingMemberTimetablesInnerMachine",
          onDone: [
            {
              target: "MEMBER_TIMETABLES_FETCHED",
              actions: (ctx, eventPayload) => {
                console.debug("Fetching done.");
                console.debug(ctx);
                console.debug(eventPayload);
              },
            },
          ],
          onError: [
            {
              target: "IDLE",
              actions: (_, eventPayload) =>
                console.warn(
                  `Fetching failed. Error: ${JSON.stringify(eventPayload, null, 2)}`
                ),
            },
          ],
        },
      },
      MEMBER_TIMETABLES_FETCHED: {
        after: {
          "bridge": "IDLE", // small bridge timespan
        },
      },
      IDLE: {
        entry: assign({
          fromDate: undefined,
          toDate: undefined,
        }),
        // identical to the specs of the initial state `DATE_RANGE_UNDEFINED`
        always: {
          target: "MEMBER_TIMETABLES_FETCHING",
          cond: "dateRangeDefined",
        },
        on: {
          DEFINE_FROM_DATE: {
            actions: [
              "assignFromDate",
              (ctx, _) => console.debug(`fromDate: ${ctx.fromDate}`),
            ],
          },
          DEFINE_TO_DATE: {
            actions: [
              "assignToDate",
              (ctx, _) => console.debug(`toDate: ${ctx.toDate}`),
            ],
          },
        },
      },
    },
  }).withConfig({
    actions: actions,
    guards: guards,
    delays: delays,
  });

  return interpret(machine).onTransition((state) => {
    if (state.value === "MEMBER_TIMETABLES_FETCHING") {
      console.debug(state.value);
      onTimetablesFetchingStarted?.();
      return;
    }
    if (state.value === "MEMBER_TIMETABLES_FETCHED") {
      console.debug(state.value);
      onTimetablesFetched?.();
      return;
    }
    console.debug(`No listener on transition to: ${state.value}`);
  });
}

function weekdayNameFromIndex(index) {
  console.assert(typeof index == "number", { index });
  console.assert(index >= 0 && index <= 7, { index });
  console.assert(!!window.dayjs?.().$locale?.());

  return dayjs().$locale().weekdays[index % 7];
}
