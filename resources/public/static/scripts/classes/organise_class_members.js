"use strict";

console.debug(`BulmaTagsInput ctor imported: ${!!window.BulmaTagsInput}`);
console.debug(`'ramda' imported: ${!!window.R}`);
console.debug(`'layouts' loaded: ${!!window.layouts}`);
console.debug(`'preact' loaded: ${!!window.preact}`);
console.debug(`'preactHooks' loaded: ${!!window.preactHooks}`);
console.debug(`htm imported: ${!!window.htm}`);

const elems = {
  heroBannerSection: undefined,
  bannerProgressBar: undefined,
  bannerDynamicContentHolder: undefined,
  formGroupDiv: undefined,
};
const masterData = {
  className: undefined,
};
const services = {
  html: undefined,
};

window.addEventListener("load", async () => {
  const mainDataset = document.querySelector("main")?.dataset;
  masterData.className = mainDataset?.["className"];
  if (!masterData.className) {
    console.debug(`className unknown. Nothing should happen.`);
    return;
  }

  elems.heroBannerSection = document.querySelector(`section#hero-banner`);
  elems.bannerDynamicContentHolder =
    elems.heroBannerSection.querySelector(`:scope p.dynamic`);
  elems.bannerProgressBar = document.querySelector(`progress.progress`);
  elems.formGroupDiv = document.querySelector(`div#dynamic-form-group`);

  const { studentsRespBody, teachersRespBody, classMembersRespBody } =
    await asyncQueryInitialData(masterData.className);
  if (!studentsRespBody || !teachersRespBody || !classMembersRespBody) {
    const message =
      "Failed to prepare data needed for this page to function properly." +
      "\nIf reloading the page doesn't fix this issue, please contact your admin.";
    layouts.renderErrorContentBody(
      { title: message },
      elems.bannerDynamicContentHolder
    );
    transformBannerToDanger(elems.heroBannerSection);
    elems.bannerProgressBar.remove();
    return;
  }
  elems.bannerProgressBar.remove();

  const prepedData = prepareFormSourceData({
    allStudents: studentsRespBody,
    allTeachers: teachersRespBody,
    currentTeacher: classMembersRespBody["classTeacher"],
    currentStudents: classMembersRespBody["classStudents"],
  });

  services.html = htm.bind(preact.h);
  const { FormGroupComponent } = defineInternalSymbols(services, masterData);

  preact.render(
    preact.h(FormGroupComponent, {
      ...prepedData,
      teacherControlFieldId: "teacher-control-field",
      studentsControlFieldId: "students-control-field",
      onSuccess(resp) {
        transformBannerToSuccess(elems.heroBannerSection);
        // TODO: to improve the message
        layouts.renderSuccessContentBody(
          { title: "Class members updated successfully." },
          elems.bannerDynamicContentHolder
        );
      },
      onValidationErrors(errors) {
        transformBannerToDanger(elems.heroBannerSection);
        layouts.renderErrorContentBody(
          { title: "Invalid request", errors: errors },
          elems.bannerDynamicContentHolder
        );
      },
      onServerErrors({ title, errors }) {
        transformBannerToDanger(elems.heroBannerSection);
        layouts.renderErrorContentBody(
          { title: title, errors: errors },
          elems.bannerDynamicContentHolder
        );
      },
      onBeforeSubmit() {
        revertBanner({
          heroBannerSection: elems.heroBannerSection,
          heroDynamicContentHolder: elems.bannerDynamicContentHolder,
        });
      },
    }),
    elems.formGroupDiv
  );
});

function defineInternalSymbols({ html }, { className }) {
  const { useEffect, useRef, useState } = preactHooks;
  const { h } = preact;

  return {
    FormGroupComponent,
  };

  function FormGroupComponent({
    teachers,
    students,
    teacherControlFieldId,
    studentsControlFieldId,
    onSuccess,
    onValidationErrors,
    onServerErrors,
    onBeforeSubmit,
  }) {
    const studentTagsController = useRef(undefined);
    const teacherTagsController = useRef(undefined);
    const [isSubmitProcessing, setSubmitProcessing] = useState(false);

    useEffect(() => {
      studentTagsController.current = document
        .querySelector(`#${studentsControlFieldId} select`)
        ?.BulmaTagsInput();
      teacherTagsController.current = document
        .querySelector(`#${teacherControlFieldId} select`)
        ?.BulmaTagsInput();
      console.assert(!!studentTagsController.current);
      console.assert(!!teacherTagsController.current);
    }, []);

    function handleSubmit(e) {
      e.preventDefault();
      setSubmitProcessing(true);
      onBeforeSubmit();

      const selectedStudents = studentTagsController.current.items;
      const selectedTeacher = teacherTagsController.current.items[0];

      if (!selectedTeacher) {
        onValidationErrors({ teacher: ["ONE teacher must be assigned."] });
        return;
      }

      asyncPutClassMemberRequest(className, {
        classTeacher: selectedTeacher, // { username: string }
        classStudents: selectedStudents, // [{ username: string }]
      })
        .then((_) => _)
        .then(({ respCode, respBody }) => {
          if (respCode >= 200 && respCode < 300 && respBody) {
            console.dir(respBody);
            onSuccess(respBody);
          } 
          else if (respCode >= 401 && respCode <= 422 && respBody) {
            console.debug(JSON.stringify(respBody, null, 2));
            onValidationErrors(respBody);
          } 
          else if (respCode >= 500 && respBody) {
            console.debug(JSON.stringify(respBody, null, 2));
            onServerErrors(respBody);
          }
          setSubmitProcessing(false);
        });
    }

    return html`
      <div>
        <section id="form-container" className="section">
          <div className="content">
            <div className="block">
              <h2 className="icon-text">
                <span className="icon">
                  <i className="fas fa-chalkboard-user"></i
                ></span>
                <span>Teacher</span>
              </h2>
              ${preact.h(TagsSelectComponent, {
                fieldId: teacherControlFieldId,
                source: teachers,
                maxTags: 1,
                itemValue: "username",
                itemText: "displayName",
                itemSelected: "inThisClass",
                noResultsLabel: "No teachers found for this search input",
                placeholder: "Select 1 teacher",
              })}
            </div>
            <br />
            <div className="block">
              <h2 className="icon-text">
                <span className="icon">
                  <i className="fas fa-users-between-lines"></i
                ></span>
                <span>Students</span>
              </h2>
              ${preact.h(TagsSelectComponent, {
                fieldId: studentsControlFieldId,
                source: students,
                isMultiple: true,
                itemValue: "username",
                itemText: "displayName",
                itemSelected: "inThisClass",
                noResultsLabel: "No students found for this search input",
                placeholder: "Select students",
              })}
            </div>
          </div>
        </section>
        <section className="section" id="button-container">
          <div class="level">
            <div class="level-left"></div>
            <div class="level-right">
              <p class="level-item">
                <button
                  class="is-primary button ${isSubmitProcessing ? " is-loading" : ""}" 
                  disabled=${isSubmitProcessing}
                  id="submit-btn"
                  onClick=${handleSubmit}
                >
                  <span class="icon is-small">
                    <i class="fas fa-check"></i>
                  </span>
                  <span><strong>Submit</strong></span>
                </button>
              </p>
            </div>
          </div>
        </section>
      </div>
    `;
  }

  function TagsSelectComponent(props) {
    const { fieldId, source, isMultiple, itemText, itemValue, itemSelected } = props;
    const selectElemRef = useRef(undefined);
    const tagsInputController = useRef(undefined);

    // at first mount
    useEffect(() => {
      console.debug(fieldId);

      const controllerOpts = {
        source: source,
        allowDuplicates: false,
        highlightDuplicate: true,
        freeInput: false,
        itemValue: itemValue, // !important
        itemText: itemText, // !important
        placeholder: props.placeholder || undefined,
        searchMinChars: 0,
        caseSensitive: props.caseSensitive || false,
        clearSelectionOnTyping: props.clearSelectionOnTyping || false,
        closeDropdownOnItemSelect: props.closeDropdownOnItemSelect || true,
        delimiter: props.delimiter || ",",
        highlightMatchesString: props.highlightMatchesString || true,
        maxTags: props.maxTags || undefined,
        noResultsLabel: props.noResultsLabel || "No results found",
        removable: props.removable || true,
        selectable: props.selectable || false,
        searchOn: props.searchOn || "text",
        tagClass: props.tagClass || "is-rounded",
        trim: props.trim || true,
      };
      
      // GOTCHA: `BulmaTagsInput.attach` returns an array of instances it created
      tagsInputController.current = BulmaTagsInput.attach(
        selectElemRef.current,
        controllerOpts
      )[0];
      
      tagsInputController.current.add(R.filter(it => it[itemSelected])(source));
    }, []);

    return html`
      <div id=${fieldId} className="field">
        <div className="${"control"}">
          <select
            ref=${selectElemRef}
            data-type="tags"
            multiple=${!!isMultiple}
          >
            ${R.map((entry) => {
              preact.h(
                "option",
                {
                  value: entry[itemValue],
                  selected: entry[itemSelected],
                },
                entry[itemText]
              );
            })(source)}
          </select>
        </div>
      </div>
    `;
  }
}

function prepareFormSourceData({
  allStudents,
  allTeachers,
  currentTeacher,
  currentStudents,
}) {
  const teachers = R.map((tch) => {
    const entry = {
      username: tch.username,
      displayName: tch.displayName,
      inThisClass: tch.username === currentTeacher?.username,
    };
    if (entry.inThisClass) {
      console.debug(entry);
    }
    return entry;
  })(allTeachers);

  const currentStudentGroups = R.groupBy(R.prop("username"), currentStudents);
  const students = R.map((std) => {
    const entry = {
      username: std.username,
      displayName: std.displayName,
      inThisClass: !!currentStudentGroups[std.username],
    };
    if (entry.inThisClass) {
      console.debug(entry);
    }
    return entry;
  })(allStudents);

  return { teachers, students };
}

/* ASSUMPTIONS: 
    - Responses from backend always include 'content-type' header
 */

async function asyncQueryInitialData(className) {
  const resps = await Promise.all([
    asyncQueryTeachers(),
    asyncQueryStudents(),
    asyncQueryExistingMembers(className),
  ]).then((_) => _); // resolves the 3 respBody parsings to get to the last level of nested promises

  return R.mergeAll(resps);
}

async function asyncQueryStudents() {
  const resp = await fetch("/api/v1/students/");
  const respBody = await (resp.headers
    .get("content-type")
    .indexOf("application/json") > -1
    ? resp.json()
    : Promise.resolve(undefined));

  if (!resp.ok) {
    console.debug(
      `Request for all students responded with code ${resp.status}`
    );
    if (!!respBody) {
      // pretty-print the response body
      console.debug(JSON.stringify(respBody, null, 2));
    }
  }

  return { studentsRespCode: resp.status, studentsRespBody: respBody };
}

async function asyncQueryTeachers() {
  const resp = await fetch("/api/v1/teachers/");
  const respBody = await (resp.headers
    .get("content-type")
    .indexOf("application/json") > -1
    ? resp.json()
    : Promise.resolve(undefined));

  if (!resp.ok) {
    console.debug(
      `Request for all teachers responded with code ${resp.status}`
    );
    if (!!respBody) {
      // pretty-print the response body
      console.debug(JSON.stringify(respBody, null, 2));
    }
  }

  return { teachersRespCode: resp.status, teachersRespBody: respBody };
}

async function asyncQueryExistingMembers(className) {
  const uri = `/api/v1/classes/${className}/members`;
  const resp = await fetch(uri);
  const respBody = await (resp.headers
    .get("content-type")
    .indexOf("application/json") > -1
    ? resp.json()
    : Promise.resolve(undefined));

  if (!resp.ok) {
    console.error(
      `Request for current class members responded with code ${resp.status}`
    );
    if (!!respBody) {
      // pretty-print the response body
      console.debug(JSON.stringify(respBody, null, 2));
    }
  }

  return {
    classMembersRespCode: resp.status,
    classMembersRespBody: respBody,
  };
}

async function asyncPutClassMemberRequest(className, requestBody) {
  const resp = await fetch(`/api/v1/classes/${className}/members`, {
    method: "PUT",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify(requestBody),
  });
  const respBody = await (resp.headers
    .get("content-type")
    .indexOf("application/json") > -1
    ? resp.json()
    : Promise.resolve(undefined));

  if (!resp.ok) {
    console.error(
      `Request for current class members responded with code ${resp.status}`
    );
    if (!!respBody) {
      // pretty-print the response body
      console.debug(JSON.stringify(respBody, null, 2));
    }
  }

  return {
    respCode: resp.status,
    respBody: respBody,
  };
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
