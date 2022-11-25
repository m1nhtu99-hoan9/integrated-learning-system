"use strict";

console.debug(`'ramda' imported: ${!!window.R}`);
console.debug(`'preact' loaded: ${!!window.preact}`);
console.debug(`'preactHooks' loaded: ${!!window.preactHooks}`);
console.debug(`'htm' imported: ${!!window.htm}`);
console.debug(`'slugify' fn imported: ${!!window.slugify}`);

console.debug(`'layouts' loaded: ${!!window.layouts}`);

const elems = {
  heroBannerSection: undefined,
  bannerProgressBar: undefined,
  bannerDynamicContentHolder: undefined,
  formGroupDiv: undefined,
};
const masterData = {
  className: undefined,
  timeslotNumber: undefined,
  schoolDate: undefined,
};
const services = {
  html: undefined,
};

window.addEventListener("load", async () => {
  if (!collectMasterData(masterData)) {
    return;
  }

  elems.heroBannerSection = document.querySelector(`section#hero-banner`);
  elems.bannerDynamicContentHolder =
    elems.heroBannerSection.querySelector(`:scope p.dynamic`);
  elems.bannerProgressBar = document.querySelector(`progress.progress`);
  elems.formGroupDiv = document.querySelector(`div#dynamic-form-group`);

  elems.bannerProgressBar.remove();

  services.html = htm.bind(preact.h);
  const { QuestionGroupComponent } = defineInternalSymbols(
    services,
    masterData
  );

  preact.render(preact.h(QuestionGroupComponent, {
    onSuccess(resp) {
      let respCount = R.length(resp);
      if (respCount === NaN) {
        respCount = 0;
      }

      transformBannerToSuccess(elems.heroBannerSection);
      layouts.renderSuccessContentBody(
        { title: `${respCount} question(s) added for homework.` },
        elems.bannerDynamicContentHolder
      );

      // TRICKY: manipulating DOM elements being managed by preact
      // make all text input controllers readonly
      document.querySelectorAll(`input.input`).forEach(inp => inp.toggleAttribute('readonly'));
    },
    onValidationErrors({title, errors}) {
      transformBannerToDanger(elems.heroBannerSection);
      layouts.renderErrorContentBody(
        { title: title ?? "Invalid request", errors: errors },
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
    }
  }), elems.formGroupDiv);
});

function collectMasterData(masterData) {
  const mainDataset = document.querySelector("main")?.dataset;
  masterData.className = mainDataset?.["className"];
  masterData.timeslotNumber = mainDataset?.["timeslotNumber"];
  masterData.schoolDate = mainDataset?.["schoolDate"];

  if (!masterData.className) {
    console.debug(`className unknown. Nothing should happen.`);
    return false;
  }
  if (!masterData.timeslotNumber || !masterData.schoolDate) {
    console.debug(
      `Either timeslotNumber or schoolDate is missing. The exact class period is unknown. Nothing should happen.`
    );
    return false;
  }
  return true;
}

function defineInternalSymbols({ html }, masterData) {
  const { useEffect, useRef, useState, useCallback } = preactHooks;
  const { h } = preact;

  // limit at 6 options
  const optionNames = ["A", "B", "C", "D", "E", "F"];

  return {
    QuestionGroupComponent,
  };

  function QuestionGroupComponent({
    onSuccess,
    onServerErrors,
    onBeforeSubmit,
    onValidationErrors
  }) {
    const inputRef = useRef(undefined);
    /* 
      question = { questionTitle, questionSlug, questionNumber, questionOption[] } 
      questionOption = { optionName, optionContent, isRight }
    */
    const [questions, setQuestions] = useState([]);

    const [isFormSubmitProcessing, setFormSubmitProcessing] = useState(false);
    const [submitBtnDisplayed, setSubmitBtnDisplayed] = useState(true);

    useEffect(() => {
      console.debug(questions);
    }, [questions]);

    function handleQuestionInputEnter() {
      const inputValue = R.when(
        (it) => !R.isNil(it),
        R.trim
      )(inputRef.current.value);

      if (inputValue.length == 0) {
        console.debug(
          `Attempt to input empty question title detected. Nothing happened.`
        );
        return;
      }

      const number = questions.length + 1;
      const title = `Question ${number}: ${inputValue}`;

      const newQuestion = {
        questionTitle: title,
        questionSlug: slugify(R.toLower(title)),
        questionNumber: number,
        questionOptions: [],
      };
      setQuestions(R.append(newQuestion, questions));

      // cleanup: reset the input
      inputRef.current.value = "";
    }

    function updateQuestionOptions(question, updatedOptions) {
      const updatedQuestions = R.map((q) => {
        if (q == question) {
          q.questionOptions = updatedOptions;
        }
        return q;
      })(questions);

      setQuestions(updatedQuestions);
    }

    function validateQuestions(questions) {
      // ASSUMPTION: `questionNumber` is unique among `questions`
      return R.reduce((errorDict, q) => {
        const qIdentifier = `Question ${q.questionNumber}`;
        const opts = q?.questionOptions;

        if (!opts || opts.length === 0) {
          errorDict[qIdentifier] = ["No answers defined yet."];
          return errorDict;
        }
        if (R.all(o => !o.isRight)(opts)) {
          errorDict[qIdentifier] = ["No answers elected as the right one yet."]
          return errorDict; 
        }
        return errorDict;
      }, ({}))(questions);  
    }

    const renderQuestionComponents = useCallback(() => {
      return R.map((q) => {
        const { questionTitle, questionSlug } = q;
        const onOptionsUpdated = useCallback(
          (questionOpts) => {
            updateQuestionOptions(q, questionOpts);
          },
          [questions, q]
        );

        return html`
          <${QuestionComponent}
            questionTitle=${questionTitle}
            questionSlug=${questionSlug}
            onOptionsUpdated=${onOptionsUpdated}
          />
        `;
      })(questions);
    }, [questions]);

    const isNotReadyForSubmit = useCallback(() => {
      if (isFormSubmitProcessing) {
        console.debug("Previous request is still being processed.");
        return true;
      }
      if (R.isEmpty(questions)) {
        console.debug("No questions created yet.");
        return true;
      }
      return false; 
    }, [isFormSubmitProcessing, questions]);

    const handleSubmit = useCallback(
      (e) => {
        e.preventDefault();
        setFormSubmitProcessing(true);
        onBeforeSubmit?.();

        // pre-validation
        const validationErrors = validateQuestions(questions);
        if (!R.isEmpty(validationErrors)) {
          console.debug(`Pre-validation found errors: ${JSON.stringify(validationErrors, null, 2)}`);
          onValidationErrors?.({ errors: validationErrors });
          setFormSubmitProcessing(false);
          return;
        }

        asyncPostAddMultipleChoiceQuestionHomework(questions, masterData)
          .then((_) => _)
          .then(({ respCode, respBody }) => {
            if (respCode >= 200 && respCode < 300 && respBody) {
              console.dir(respBody);
              onSuccess(respBody);
              setSubmitBtnDisplayed(false);
            } 
            else if (respCode >= 401 && respCode <= 422 && respBody) {
              console.debug(JSON.stringify(respBody, null, 2));
              onValidationErrors?.(respBody);
              setFormSubmitProcessing(false);
            } 
            else if (respCode >= 500 && respBody) {
              console.debug(JSON.stringify(respBody, null, 2));
              onServerErrors(respBody);
              setFormSubmitProcessing(false);
            }
          });
      },
      [questions]
    );

    return html`
    <div>
      <section className="section">
        <div class="columns">
          <div class="content column is-11">
            ${renderQuestionComponents()}
            <br />
            <div>
              <input
                ref=${inputRef}
                className="input"
                placeholder="New question"
                type="text"
                onKeyPress=${(e) =>
                  e.key === "Enter" && handleQuestionInputEnter()}
                
              />
            </div>
          </div>
        </div>
      </section>
      <section className="section" id="button-container">
        <div className="level">
          <div className="level-left"></div>
          <div className="level-right">
            <p className="level-item">
              ${submitBtnDisplayed && html`
                <button 
                  className=${"button is-primary" + (isFormSubmitProcessing ? " is-loading" : "")}
                  disabled=${isNotReadyForSubmit()}
                  onClick=${handleSubmit}
                >
                  <span className="icon is-small"><i className="fas fa-check"/></span>
                  <span><strong>Submit</strong></span>
                </button>
              `}
            </p>
          </div>
        </div>
      </section>
    </div>
    `;
  }

  function QuestionComponent(props) {
    const questionOptionInputRef = useRef(undefined);
    const { questionTitle, questionSlug, onOptionsUpdated } = props;

    const [questionOptions, setQuestionOptions] = useState([]);
    const [nextOptionName, setNextOptionName] = useState(
      getNextOptionName(questionOptions)
    );

    useEffect(() => {
      setNextOptionName(getNextOptionName(questionOptions));
      onOptionsUpdated?.(questionOptions);
    }, [questionOptions]);

    function handleQuestionOptionInputEnter() {
      const inputValue = R.when(
        (it) => !R.isNil(it),
        R.trim
      )(questionOptionInputRef.current.value);

      if (inputValue.length == 0) {
        console.debug(
          `Attempt to input empty question option detected. Nothing happened.`
        );
        return;
      }

      const optionName = getNextOptionName(questionOptions);
      const newOption = {
        optionName: optionName,
        optionContent: inputValue,
        isRight: false,
      };

      setQuestionOptions(R.append(newOption, questionOptions));

      // cleanup: reset the input
      questionOptionInputRef.current.value = "";
    }

    // ASSUMPTION: `currentQuestionOptions` are already sorted by `optionName`
    function getNextOptionName(currentQuestionOptions) {
      const currentOptionNum = R.length(currentQuestionOptions);

      if (currentOptionNum === optionNames.length) {
        console.warn(`Option limit reached.`);
        return undefined;
      }
      return optionNames[currentOptionNum];
    }

    function removeOption(option) {
      const options = R.without([option], questionOptions);
      // re-assign option names
      const rearrangedOptions = R.pipe(R.sortBy(R.prop("optionName")), (opts) =>
        opts.map((opt, i) => R.assoc("optionName", optionsNames[i], opt))
      )(options);

      setQuestionOptions(rearrangedOptions);
    }

    function chooseRightOption(option) {
      const rearrangedOptions = questionOptions.map((opt) =>
        R.assoc("isRight", opt == option, opt)
      );
      setQuestionOptions(rearrangedOptions);
    }

    const renderOptionRows = useCallback(() => {
      return R.map((option) => {
        const { optionName, optionContent } = option;
        const handleOptionSelect = useCallback(() => {
          chooseRightOption(option);
        }, [option]);
        const handleOptionRemove = useCallback(() => {
          removeOption(option);
        }, [option]);

        return html`
          <${QuestionOptionRowComponent}
            optionGroupName=${questionSlug}
            optionName=${optionName}
            content=${optionContent}
            handleOptionRemove=${handleOptionRemove}
            handleOptionSelect=${handleOptionSelect}
          />
        `;
      })(questionOptions);
    }, [questionOptions]);

    return html`
      <div>
        <br/>
        <h4>${questionTitle}</h4>
        <table className="table is-completely-borderless">
          <tbody>
            ${renderOptionRows()}
            <tr>
              <td className="question-answer-option">
                ${nextOptionName &&
                html`
                  <div className="field">
                    <label className="b-radio radio question-option-radio">
                      <input
                        name=${questionSlug}
                        type="radio"
                        value=${nextOptionName}
                      />
                      <span className="check"> </span>
                      <span className="control-label">${nextOptionName}</span>
                    </label>
                  </div>
                `}
              </td>
              <td className="question-answer-content">
                <input
                  ref=${questionOptionInputRef}
                  class="input"
                  placeholder="Question choice"
                  type="text"
                  onKeyPress=${(e) =>
                    e.key === "Enter" && handleQuestionOptionInputEnter()}
                  disabled=${!nextOptionName}
                />
              </td>
              <td className="question-answer-actions"></td>
            </tr>
          </tbody>
        </table>
      </div>
    `;
  }

  function QuestionOptionRowComponent(props) {
    const {
      optionName,
      optionGroupName,
      content,
      handleOptionSelect,
      handleOptionRemove,
    } = props;

    function onOptionSelected(e) {
      handleOptionSelect?.();
    }

    function onOptionRemoveAction(e) {
      handleOptionRemove?.();
    }

    return html`
      <tr>
        <td className="question-answer-option">
          <label
            className="b-radio radio question-option-radio"
            onClick=${onOptionSelected}
          >
            <input name=${optionGroupName} type="radio" value=${optionName} />
            <span class="check"></span>
            <span class="control-label">${optionName}</span>
          </label>
        </td>
        <td class="question-answer-content">
          <span>${content}</span>
        </td>
        <td class="question-answer-actions">
          <button
            class="button is-danger is-outlined"
            onClick=${onOptionRemoveAction}
          >
            <span class="icon is-small">
              <i class="fas fa-trash"></i>
            </span>
          </button>
        </td>
      </tr>
    `;
  }
}

async function asyncPostAddMultipleChoiceQuestionHomework(
  questions,
  { className, timeslotNumber, schoolDate }
) {
  const requestBody = { multipleChoiceQuestions: questions };
  const resp = await fetch(
    `/api/v1/classes/${className}/periods/${schoolDate}/${timeslotNumber}/homework`,
    {
      method: "POST",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(requestBody),
    }
  );
  const respBody = await (resp.headers
    .get("content-type")
    .indexOf("application/json") > -1
    ? resp.json()
    : Promise.resolve(undefined));

  if (!resp.ok) {
    console.error(
      `Request for adding homework questions responded with code ${resp.status}`
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