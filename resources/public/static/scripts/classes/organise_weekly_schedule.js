"use strict";

console.debug(`'vanillajs-datepicker':` +
    `\n\tDateRangePicker ctor imported: ${!!window.DateRangePicker}`
);
console.debug(`'ramda' imported: ${!!window.R}`);
console.debug(`'layouts' loaded: ${!!window.layouts}`);

const masterData = {
  className: undefined,
};
const elems = {
  heroBannerSection: undefined,
  heroDynamicContentHolder: undefined,
  startDateInput: undefined,
  endDateInput: undefined,
  dateRangeInputContainer: undefined,
  submitButton: undefined,
  resetButton: undefined,
  checkboxAggregates: [],
};
const services = { dateRangePicker: undefined };

window.addEventListener("load", function (_) {
  const DateRangePicker = window.DateRangePicker;
  const mainDataset = document.querySelector("main")?.dataset;
  masterData.className = mainDataset?.["className"];
  masterData.postUri = mainDataset?.["postV1Uri"];

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

  R.forEach(defineCheckboxAggregateEventHandlers, elems.checkboxAggregates);

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
      handleErrorResp({ 
        title: "Invalid request data",
        errors: validationErrors
      }, elems);
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
        resp.json().then(data => handleOkResp(data, elems));
        toggleEditability(elems);
        makeReadonly(R.assoc("dateRangePicker", services.dateRangePicker, elems));
        return;
      } 
      if (resp.status >= 400) {
        resp.json().then(data => handleErrorResp(data, elems));
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
});

window.addEventListener("beforeunload", function (_) {
  services.dateRangePicker?.destroy();
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
    errors.startDate = [ "Start date cannot be empty." ];
  }
  if (!endDate) {
    errors.endDate = [ "End date cannot be empty." ];
  }

  return errors;
}

function handleOkResp(respBody, { heroBannerSection, heroDynamicContentHolder }) {
  transformBannerToSuccess(heroBannerSection);
  
  console.dir(respBody);
  const periodsCount = R.length(respBody); 
  console.assert(periodsCount > 0);
  const message = `${isNaN(periodsCount) ? 0 : periodsCount} class periods created.`
  
  layouts.renderSuccessContentBody({title: message}, heroDynamicContentHolder);
}

function handleErrorResp({ title, errors }, { heroBannerSection, heroDynamicContentHolder }) {
  transformBannerToDanger(heroBannerSection);
  layouts.renderErrorContentBody({ title, errors }, heroDynamicContentHolder);
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
  submitButton, resetButton,
  startDateInput, endDateInput,
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
  submitButton, resetButton,
  startDateInput, endDateInput,
  checkboxAggregates,
  dateRangePicker
}) {
  submitButton?.setAttribute("disabled", "");
  resetButton?.setAttribute("disabled", "");
  
  startDateInput?.setAttribute("readonly", "");
  endDateInput?.setAttribute("readonly", "");
  dateRangePicker?.destroy();
  
  R.forEach(({ spanElem }) => spanElem.addEventListener("click", e => e.preventDefault()), checkboxAggregates);
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
