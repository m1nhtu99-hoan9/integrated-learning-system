"use strict";

console.debug(
  `'vanillajs-datepicker':` +
    `\n\tDatepicker imported: ${!!window.Datepicker}` +
    `\n\tDateRangePicker ctor imported: ${!!window.DateRangePicker}`
);
console.debug(`'ramda' imported: ${!!window.R}`);
console.debug(`'preact' loaded: ${!!window.preact}`);
console.debug(`'htm' imported: ${!!window.htm}`);
console.debug(
  `'dayjs' imported: ${!!window.dayjs}.\n\t'isSameOrAfter' plugin imported: ${!!window.dayjs_plugin_isSameOrAfter}.`
);
console.debug(`'bulma-toast' imported: ${!!window.bulmaToast}`);

console.debug(`'layouts' loaded: ${!!window.layouts}`);
console.debug(
  `'createCurrentMemberTimetableStateMachine' loaded: ${!!window.createCurrentMemberTimetableStateService}`
);

window.dayjs?.extend(window.dayjs_plugin_isSameOrAfter);

const masterData = {
  className: undefined,
};
const elems = {
  heroBannerSection: undefined,
  heroDynamicContentHolder: undefined,
  startDateInput: undefined,
  endDateInput: undefined,
  dateRangeInputContainer: undefined,
  table: undefined,
  submitButton: undefined,
  resetButton: undefined,
  checkboxAggregates: [],
};
const services = {
  dateRangePicker: undefined,
  memberTimetableStateService: undefined,
};

window.addEventListener("load", function (_) {
  const DateRangePicker = window.DateRangePicker;

  const mainDataset = document.querySelector("main")?.dataset;
  masterData.className = mainDataset?.["className"];
  masterData.postUri = mainDataset?.["postV1Uri"];
  if (!masterData.className) {
    console.warn("Class name undefined. Nothing should happen.");
    return;
  }

  queryThenDefineElems(elems);
  if (!!elems.dateRangeInputContainer) {
    services.dateRangePicker = new DateRangePicker(
      elems.dateRangeInputContainer,
      {
        format: "dd/mm/yyyy",
        todayBtn: true,
        todayBtnMode: 1, // https://mymth.github.io/vanillajs-datepicker/#/options?id=todaybtnmode
        calendarWeeks: true,
      }
    );
  }
  elems.startDateInput = services.dateRangePicker?.inputs[0];
  elems.endDateInput = services.dateRangePicker?.inputs[1];

  elems.table?.classList.add('is-loading');
  const resetValueIfNotNull = R.when(
    R.complement(R.isNil),
    (x) => (x.value = "")
  );
  resetValueIfNotNull(elems.startDateInput);
  resetValueIfNotNull(elems.endDateInput);

  services.memberTimetableStateService =
    createCurrentMemberTimetableStateService({
      className: masterData.className,
      onTimetablesFetchingStarted: () => {
        R.when(
          (classes) => !R.isNil(classes) && !classes.contains("is-loading"),
          (classes) => classes.toggle("is-loading")
        )(elems.table?.classList);
      },
      onTimetablesFetched: () => {
        // CAUTION: object path depends on the machine's context schema
        const machineClassContext =
          services.memberTimetableStateService.machine.context.class;
        /* re-arrange selection table, then re-enable the table's usability */
        const overlapTimetableEntries = evaluateOverlapTimetableEntries(
          {
            fromDate: new Date(
              Datepicker.parseDate(elems.startDateInput.value, "dd/mm/yyyy")
            ),
            toDate: new Date(
              Datepicker.parseDate(elems.endDateInput.value, "dd/mm/yyyy")
            ),
          },
          machineClassContext
        );
        console.debug(JSON.stringify(overlapTimetableEntries, null, 2));
        const { checkableTdElems } = rearrangeSelectionTable(
          elems.table,
          machineClassContext,
          overlapTimetableEntries
        );

        // re-query, then re-define event listeners, for `td`s
        elems.checkboxAggregates = R.map(
          getCheckboxElemAggregate,
          checkableTdElems
        );
        R.forEach(
          defineCheckboxAggregateEventHandlers,
          elems.checkboxAggregates
        );
        R.when(
          (classes) => !R.isNil(classes),
          (classes) => classes.remove("is-loading")
        )(elems.table?.classList);
      },
    });

  const onDateValuesChanges = function () {
    const prepDateString = function (d) {
      if (!d) {
        return null;
      }
      // https://mymth.github.io/vanillajs-datepicker/#/date-string+format?id=date-format
      return Datepicker.formatDate(
        Datepicker.parseDate(d, "dd/mm/yyyy"),
        "yyyy-mm-dd"
      );
    };
    services.memberTimetableStateService.send("DEFINE_FROM_DATE", {
      value: prepDateString(elems.startDateInput?.value),
    });
    services.memberTimetableStateService.send("DEFINE_TO_DATE", {
      value: prepDateString(elems.endDateInput?.value),
    });
  };
  // https://mymth.github.io/vanillajs-datepicker/#/api?id=events
  // https://github.com/mymth/vanillajs-datepicker/blob/v1.2.0/test/events.js#L22
  elems.startDateInput?.addEventListener("changeDate", (_) =>
    onDateValuesChanges()
  );
  elems.endDateInput?.addEventListener("changeDate", (_) =>
    onDateValuesChanges()
  );

  services.memberTimetableStateService.start();

  R.forEach(defineCheckboxAggregateEventHandlers, elems.checkboxAggregates);

  // <editor-fold desc="event handling for buttons">

  elems.submitButton?.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (elems.submitButton.classList.contains("is-loading")) {
      return;
    }
    if (!masterData.postUri) {
      console.error("API endpoint unknown. Try reloading...");
      window.location.reload();
    }

    const requestBody = collectRequestPayload(elems);
    console.dir(requestBody);
    const validationErrors = getRequestValidationErrors(requestBody);
    if (!R.isEmpty(validationErrors)) {
      handleErrorResp(
        {
          title: "Invalid request data",
          errors: validationErrors,
        },
        elems
      );
      return;
    }

    toggleEditability(elems);
    revertBanner(elems);

    console.debug(`Sending POST: ${masterData.postUri}`);
    fetch(masterData.postUri, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(requestBody),
    }).then((resp) => {
      console.dir(resp);

      if (resp.status >= 200 && resp.status < 300) {
        resp.json().then((data) => handleOkResp(data, elems));
        toggleEditability(elems);
        makeReadonly(
          R.assoc("dateRangePicker", services.dateRangePicker, elems)
        );
        return;
      }
      if (resp.status >= 400) {
        resp.json().then((data) => handleErrorResp(data, elems));
        toggleEditability(elems);
        return;
      }
    });
  });

  elems.resetButton?.addEventListener("click", (e) => {
    e.preventDefault();
    R.forEach((aggregate) => {
      if (aggregate.inputElem?.checked) {
        aggregate.tdElem?.click();
      }
    }, elems.checkboxAggregates);
  });

  // </editor-fold>
});

window.addEventListener("beforeunload", function (_) {
  services.dateRangePicker?.destroy();
  services.memberTimetableStateService?.stop();
});

function queryThenDefineElems(elems) {
  elems.heroBannerSection = document.querySelector(`section#hero-banner`);
  elems.heroDynamicContentHolder =
    elems.heroBannerSection?.querySelector(`:scope p.dynamic`);
  elems.dataErrorBannerPlaceholder = document.querySelector(
    `.section#data-error-banner-placeholder`
  );

  elems.dateRangeInputContainer = document.querySelector(
    "#date-range-input-container"
  );
  elems.table = document.querySelector(".content table");
  elems.checkboxAggregates = R.map(
    getCheckboxElemAggregate,
    document.querySelectorAll("td")
  );

  elems.submitButton = document.querySelector(`button#submit-btn`);
  elems.resetButton = document.querySelector(`button#reset-btn`);
}

// <editor-fold desc="Calling API">

function collectRequestPayload(elems) {
  return {
    startDate: elems.startDateInput?.value,
    endDate: elems.endDateInput?.value,
    weeklySchedule: collectSelectedWeeklySlots(elems.checkboxAggregates),
  };
}

function collectSelectedWeeklySlots(checkboxAggregates) {
  return R.pipe(
    R.filter((aggregate) => aggregate.inputElem.checked),
    R.map((aggregate) => {
      const dataset = aggregate.tdElem.dataset;
      const dayOfWeekNum = parseInt(dataset["dayOfWeekNum"], 10);
      const timeslotNum = parseInt(dataset["slotNo"], 10);
      return {
        dayOfWeekNum: isNaN(dayOfWeekNum) ? null : dayOfWeekNum,
        timeslotNumber: isNaN(timeslotNum) ? null : timeslotNum,
      };
    })
  )(checkboxAggregates);
}

function getRequestValidationErrors({ startDate, endDate }) {
  const errors = {};

  if (!startDate) {
    errors.startDate = ["Start date cannot be empty."];
  }
  if (!endDate) {
    errors.endDate = ["End date cannot be empty."];
  }

  return errors;
}

function handleOkResp(
  respBody,
  { heroBannerSection, heroDynamicContentHolder }
) {
  transformBannerToSuccess(heroBannerSection);

  console.dir(respBody);
  const periodsCount = R.length(respBody);
  console.assert(periodsCount > 0);
  const message = `${
    isNaN(periodsCount) ? 0 : periodsCount
  } class periods created.`;

  layouts.renderSuccessContentBody(
    { title: message },
    heroDynamicContentHolder
  );
}

function handleErrorResp(
  { title, errors },
  { heroBannerSection, heroDynamicContentHolder }
) {
  transformBannerToDanger(heroBannerSection);
  layouts.renderErrorContentBody({ title, errors }, heroDynamicContentHolder);
}

// </editor-fold>

// <editor-fold desc="Selection table">

function evaluateOverlapTimetableEntries({ fromDate, toDate }, { timetables }) {
  console.assert(!!timetables?.classTeacher, {
    "timetables.classTeacher": timetables?.classTeacher,
  });
  console.assert(!!timetables?.classStudents, {
    "timetables.classStudents": timetables?.classStudents,
  });

  const focusedDates = dayjsDateRange({ fromDate, toDate });

  // teacher's timetable entries within given date ranges
  const teacherOverlapTimetable = R.reduce((entries, dayjsDate) => {
    const thisDateEntries = R.filter((x) => {
      if (!x?.date) {
        console.debug(`without a date: ${JSON.stringify(x, null, 2)}`);
        return false;
      }
      // https://day.js.org/docs/en/query/is-same
      return dayjsDate.isSame(x.date, "day");
    })(timetables?.classTeacher);

    if (R.length(thisDateEntries) > 0) {
      return entries.concat(thisDateEntries);
    }
    return entries;
  }, [])(focusedDates);

  // students' timetable entries within given date ranges
  const studentsOverlapTimetable = R.reduce((entries, dayjsDate) => {
    const thisDateEntries = R.filter((x) => {
      if (!x?.date) {
        console.debug(`without a date: ${JSON.stringify(x, null, 2)}`);
        return false;
      }
      return dayjsDate.isSame(x.date, "day");
    })(timetables?.classStudents);

    if (R.length(thisDateEntries) > 0) {
      thisDateEntries.forEach((entry) => {
        const payload = {
          username: entry.username,
          className: entry.className,
        };
        const existingEntry = R.find(
          (x) =>
            entry.date.isSame(x.date, "day") &&
            entry.timeSlotNumber === x.timeslotNumber
        )(entries);

        if (R.isNil(existingEntry)) {
          entries.push({
            date: entry.date,
            timeslotNumber: entry.timeslotNumber,
            weekdayName: entry.weekdayName,
            students: [payload],
          });
        } else {
          existingEntry.students.push(payload);
        }
      });
    }
    return entries;
  }, [])(focusedDates);

  return { studentsOverlapTimetable, teacherOverlapTimetable };
}

function rearrangeSelectionTable(
  tableElem,
  { classTeacher, classStudents },
  { studentsOverlapTimetable, teacherOverlapTimetable }
) {
  console.assert(!!tableElem, { tableElem });
  const tdElems = { checkableTdElems: [], uncheckableTdElems: [] };

  tableElem.querySelectorAll(`:scope td`).forEach((tdElem) => {
    const elemTimeslotNumber = tdElem.dataset["slotNo"];
    const elemWeekdayName = tdElem.dataset["dayOfWeek"];
    const _predicate = (x) =>
      x.timeslotNumber == elemTimeslotNumber &&
      x.weekdayName === elemWeekdayName;
    const teacherTimetableEntry = R.find(_predicate, teacherOverlapTimetable);
    const studentTimetableEntry = R.find(_predicate, studentsOverlapTimetable);

    const teacherTimetableMessage = R.isNil(teacherTimetableEntry)
      ? null
      : `<span>Teacher <em>${
          Object.values(classTeacher)[0]
        }</em> is not available on this slot.</span>`;
    const studentTimetableMessage = R.isNil(studentTimetableEntry)
      ? null
      : `<span>Students not available for this slot: ${studentTimetableEntry.students
          .map((x) => "<em>" + classStudents[x.username] + "</em>")
          .join(", ")}.</span>`;

    if (teacherTimetableMessage === null && studentTimetableMessage === null) {
      tdElem.innerHTML = `
        <label class="b-checkbox checkbox">
          <input type="checkbox" value="0"/>
          <span class="check is-info"></span>
        </label>
      `;
      tdElems.checkableTdElems.push(tdElem);
    } else {
      tdElem.innerHTML = `
        <span class="icon has-text-info">
          <i class="fas fa-info-circle"></i>
        </span>
      `;

      tdElem.addEventListener("click", e => {
        bulmaToast.toast({
          message:
            `<div class="content"><h3>${elemWeekdayName} Slot ${elemTimeslotNumber}</h3>` +
            [teacherTimetableMessage, studentTimetableMessage].join("<br/>") + "</div>",
          position: "top-center",
          type: "is-warning",
          duration: 8000, // in milliseconds
          dismissible: true,
        });

        e.stopPropagation();
      });

      tdElems.uncheckableTdElems.push(tdElem);
    }
  });

  return tdElems;
}

// </editor-fold>

//<editor-fold desc="Table checkboxes">

function getCheckboxElemAggregate(tdElem) {
  return {
    spanElem: tdElem?.querySelector(`:scope span.check`),
    inputElem: tdElem?.querySelector(`:scope input[type='checkbox']`),
    tdElem: tdElem,
  };
}

function defineCheckboxAggregateEventHandlers(checkboxElemAggregate) {
  const { spanElem, inputElem, tdElem } = checkboxElemAggregate;
  tdElem.addEventListener("click", (_) => {
    spanElem.click();
  });
  // prevents 'click' event bubbles up to `tdElem`
  spanElem.addEventListener("click", (e) => e.stopPropagation());
  inputElem.addEventListener("click", (e) => e.stopPropagation());
}

//</editor-fold>

function toggleEditability({
  submitButton,
  resetButton,
  startDateInput,
  endDateInput,
}) {
  submitButton?.classList.toggle("is-loading");
  R.forEach(toggleInputAccessibility, [startDateInput, endDateInput]);
  resetButton?.toggleAttribute("disabled");
}

function toggleInputAccessibility(inputElem) {
  if (!inputElem) {
    return;
  }
  inputElem.parentElement.classList.toggle("is-loading");
  inputElem.toggleAttribute("disabled");
}

function makeReadonly({
  submitButton,
  resetButton,
  startDateInput,
  endDateInput,
  checkboxAggregates,
  dateRangePicker,
}) {
  submitButton?.setAttribute("disabled", "");
  resetButton?.setAttribute("disabled", "");

  startDateInput?.setAttribute("readonly", "");
  endDateInput?.setAttribute("readonly", "");
  dateRangePicker?.destroy();

  R.forEach(
    ({ spanElem }) =>
      spanElem.addEventListener("click", (e) => e.preventDefault()),
    checkboxAggregates
  );
}

// <editor-fold desc="Banner">

function transformBannerToDanger(heroBannerSection) {
  if (!heroBannerSection) {
    return;
  }

  heroBannerSection.classList.remove("is-info");
  heroBannerSection.classList.remove("is-success");
  heroBannerSection.classList.add("is-danger");
}

function transformBannerToSuccess(heroBannerSection) {
  if (!heroBannerSection) {
    return;
  }

  heroBannerSection.classList.remove("is-info");
  heroBannerSection.classList.remove("is-danger");
  heroBannerSection.classList.add("is-success");
}

function revertBanner({ heroBannerSection, heroDynamicContentHolder }) {
  if (!heroBannerSection) {
    return;
  }

  heroBannerSection.classList.remove("is-danger");
  heroBannerSection.classList.remove("is-success");
  heroBannerSection.classList.add("is-info");

  heroDynamicContentHolder.replaceChildren(); // https://caniuse.com/?search=replaceChildren
}

// </editor-fold>

// <editor-fold desc="Misc utilities">

// sample usage: dayjsDateRange({fromDate: '2022-10-21', toDate: '2022-11-21'}).map(x => Datepicker.formatDate(x.toDate(), 'dd/mm/yyyy'))
function dayjsDateRange({ fromDate, toDate }) {
  console.assert(!R.isNil(fromDate), { fromDate });
  console.assert(!R.isNil(toDate), { toDate });

  // https://day.js.org/docs/en/query/is-a-dayjs
  const _fromDate = fromDate instanceof dayjs ? fromDate : dayjs(fromDate);
  const _toDate = toDate instanceof dayjs ? toDate : dayjs(toDate);
  // ASSUMPTION: `fromDate` is before or same as `toDate`
  console.assert(_toDate.isSameOrAfter(_fromDate), { fromDate, toDate });

  const dates = [];
  for (
    let date = _fromDate;
    _toDate.isSameOrAfter(date);
    date = date.add(1, "d")
  ) {
    dates.push(date);
  }
  return dates;
}

// </editor-fold>
