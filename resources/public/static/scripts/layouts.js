"use strict";

console.debug(`ramda imported: ${!!window.R}`);
console.debug(`preact imported: ${!!window.preact}`);
console.debug(`htm imported: ${!!window.htm}`);

const _layouts$services = {
  html: undefined,
};

window.layouts = {
  renderSuccessContentBody: undefined,
  renderErrorContentBody: undefined,
};

window.addEventListener("load", function (_) {
  const preact = window.preact;
  const htmLib = window.htm;

  _layouts$services.html = htmLib.bind(preact.h);

  window.layouts = _exportLayouts(_layouts$services);
});

function _exportLayouts(services) {
  const { html } = services;
  console.assert(!!html);
  console.assert(!!preact.h);

  function SuccessContentBody({ title }) {
    return html`
      <div class="content">
        <span class="icon-text">
          <span class="icon"><i class="fas fa-circle-check"></i></span>
          <span><strong>${title}</strong></span>
        </span>
      </div>
    `;
  }

  function ErrorContentBody({ title, errors }) {
    return html`
      <div class="content">
        <span class="icon-text">
          <span class="icon"><i class="fas fa-exclamation-circle"></i></span>
          <span><strong>${title}</strong></span>
        </span>
        <ul>
          ${R.map(
            ([ fieldName, errorMessages ]) =>
              preact.h(ErrorListItem, { fieldName, errorMessages }),
            R.toPairs(errors)
          )}
        </ul>
      </div>
    `;
  }

  function ErrorListItem({ fieldName, errorMessages }) {
    return html`
      <li>
        <code>${fieldName}</code>
        <ul>
          ${R.map((msg) => preact.h("li", null, msg), errorMessages)}
        </ul>
      </li>
    `;
  }

  return {
    renderSuccessContentBody: function(props, containerNode) {
      // render the body, then append it to the head of `containerNode`'s children nodes
      preact.render(preact.h(SuccessContentBody, props), containerNode);
    },
    renderErrorContentBody: function (props, containerNode) {
      // render the body, then append it to the head of `containerNode`'s children nodes
      preact.render(preact.h(ErrorContentBody, props), containerNode);
    },
  };
}
