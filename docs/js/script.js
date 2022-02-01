const defaultServerName = "server";

const serverNameProvider = () => {
  return (urlParams.get("server_name") || defaultServerName)
    .replaceAll(/[^a-z0-9]/g, "");
};

const urlParams = new URLSearchParams(window.location.search);
const serverName = serverNameProvider();

window.onload = () => {
  const serverNameSpans = document.querySelectorAll(".server-name");
  serverNameSpans.forEach((el) => {
    el.innerHTML = `<code class='chapter__code-block__inline'>${serverName}</code>`;
  });

  if (serverName !== defaultServerName) {
    const innerLinks = document.querySelectorAll(".inner-link");
    innerLinks.forEach((el) => {
      el.href = el.href + `?server_name=${serverName}`;
    });

    const securityImages = document.querySelectorAll(".js-security-image");
    securityImages.forEach((el) => {
      el.src = el.src.replace(".png", `.${serverName}.png`);
    });
  }
};
