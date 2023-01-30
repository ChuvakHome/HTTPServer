fetch("add_to_tbl.php")
  .then(response => response.text())
  .then(response => document.getElementById("result_table").innerHTML = response);

const R0 = 100;
const X0 = 150;
const Y0 = 150;

document.getElementById("submit_btn").addEventListener("click", submit);
document.getElementById("clear_btn").addEventListener("click", clear);

let elems = document.getElementsByTagName("input");
let len = elems.length;

var x_val, y_val, r_val;

for (i = 0; i < len; i++)
{
    elems[i].addEventListener("change", onChange);
    elems[i].addEventListener("input", onChange);
    elems[i].addEventListener("click", onClick);
}

function checkY(yText)
{
    y = yText.value.replace(',', '.').trim();

    if (y.length == 0)
    {
        yText.setCustomValidity("Укажите значение для Y-координаты");
        return false;
    }
    else if (!isFinite(y))
    {
        yText.setCustomValidity("Y-координата должна быть числом");
        return false;
    }
    else if (y < -3 || y > 3)
    {
        yText.setCustomValidity("Y-координата должна принадлежать интервалу (-3; 3)");
        return false;
    }

    yText.setCustomValidity("");
    return true;
}

function onChange(e)
{
    let point = document.getElementById("point");

    let formData = new FormData(document.getElementById("form"));
    x_val = formData.get("x");
    y_val = document.getElementById("y");

    if (!checkY(y_val))
        return;

    r_val = formData.get("r");

    let px = x_val / r_val * R0 + X0, py = Y0 - y_val.value.replace(',', '.').trim() / r_val * R0;

    point.setAttributeNS(null, "cx", px);
    point.setAttributeNS(null, "cy", py);
    point.setAttributeNS(null, "visibility", "visible");
}

function submit(e)
{
    let formData = new FormData(document.getElementById("form"));
    x_val = formData.get("x");
    y_val = document.getElementById("y");

    if (!checkY(y_val))
        return;

    y_val = y_val.value.replace(',', '.').trim();
    r_val = formData.get("r");

    e.preventDefault();

    fetch(`check.php?x=${x_val}&y=${y_val}&r=${r_val}`)
      .then(response => response.text())
      .then(response => document.getElementById("result_table").insertAdjacentHTML('beforeend', response));
}

function clear(e)
{
    e.preventDefault();

    fetch("clear_tbl.php")
      .then(response => response.text())
      .then(response => document.getElementById("result_table").innerHTML = response);
}

function onClick(e)
{
    let name = e.currentTarget.name;

    if (name === null || name.trim().length == 0 || name === undefined)
        return;

    for (i = 0; i < len; i++)
    {
        if (elems[i].name === name)
           elems[i].checked = false;
    }

    e.currentTarget.checked = true;
}
