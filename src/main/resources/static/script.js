function checkFiles(files) {
    console.log(files);

    if (files.length != 1) {
        alert("Bitte genau eine Datei hochladen.");
        return;
    }

    const fileSize = files[0].size / 1024 / 1024; // in MiB
    if (fileSize > 10) {
        alert("Datei zu groß (max. 10Mb)");
        return;
    }

    answerPart.style.visibility = "visible";
    const file = files[0];

    // Preview
    if (file) {
        preview.src = URL.createObjectURL(files[0]);
    }

    // Upload
    const formData = new FormData();
    for (const name in files) {
        formData.append("image", files[name]);
    }

    fetch('/analyze', {
        method: 'POST',
        headers: {},
        body: formData
    }).then(
        response => {
            console.log(response);
            response.json().then(function (data) {
                displayResults(data);
                drawChart(data);
            });
        }
    ).then(
        success => console.log(success)
    ).catch(
        error => console.log(error)
    );
}

function displayResults(data) {
    const answer = document.getElementById('answer');
    const table = document.createElement('table');
    table.className = 'table table-center';
    const thead = document.createElement('thead');
    const tbody = document.createElement('tbody');

    const headerRow = document.createElement('tr');
    const headers = ['Class Name', 'Probability (%)', 'Probability'];
    headers.forEach(headerText => {
        const header = document.createElement('th');
        header.scope = 'col';
        header.appendChild(document.createTextNode(headerText));
        headerRow.appendChild(header);
    });
    thead.appendChild(headerRow);

    // Sort data by probability in descending order
    data.sort((a, b) => b.probability - a.probability);

    data.forEach(item => {
        const row = document.createElement('tr');
        const cellClassName = document.createElement('td');
        const cellProbabilityPercent = document.createElement('td');
        const cellProbabilityDecimal = document.createElement('td');

        cellClassName.appendChild(document.createTextNode(item.className));
        cellProbabilityPercent.appendChild(document.createTextNode((item.probability * 100).toFixed(2) + '%'));
        cellProbabilityDecimal.appendChild(document.createTextNode(item.probability.toFixed(4)));

        row.appendChild(cellClassName);
        row.appendChild(cellProbabilityPercent);
        row.appendChild(cellProbabilityDecimal);
        tbody.appendChild(row);
    });

    table.appendChild(thead);
    table.appendChild(tbody);
    answer.innerHTML = '';
    answer.appendChild(table);
}

function drawChart(data) {
    const ctx = document.getElementById('probabilityChart').getContext('2d');
    const labels = data.map(item => item.className);
    const probabilities = data.map(item => (item.probability * 100).toFixed(2));

    new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Probability (%)',
                data: probabilities,
                backgroundColor: 'rgba(75, 192, 192, 0.2)',
                borderColor: 'rgba(75, 192, 192, 1)',
                borderWidth: 1
            }]
        },
        options: {
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        color: 'white'
                    }
                },
                x: {
                    ticks: {
                        color: 'white'
                    }
                }
            },
            plugins: {
                legend: {
                    labels: {
                        color: 'white'
                    }
                }
            }
        }
    });
}
