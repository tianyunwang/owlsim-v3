#!/usr/bin/python

import os
import getopt
import requests
import json
import csv
import shutil
import argparse

def uniqAndSort(output):
    sorted_suffix = '.sorted2'
    f = open(output, "r", encoding='utf-8')
    fs = open(output + sorted_suffix, "w+", encoding='utf-8')
    fs.writelines(sorted(set(f.readlines())))
    shutil.move(output + sorted_suffix, output)

def transformLabel(data, output):

    with open(output, 'w', encoding='utf-8') as tsvfile:
        writer = csv.writer(tsvfile, delimiter='\t')
        for entry in data:
            id = entry["subject"]
            label = entry["subject_label"]
            writer.writerow([id, label])

    uniqAndSort(output)

def transformAssociation(data, output):

    with open(output, 'w', encoding='utf-8') as tsvfile:
        writer = csv.writer(tsvfile, delimiter='\t')
        for entry in data:
            id = entry["subject"]
            for obj in entry["objects"]:
                writer.writerow([id, obj])

    uniqAndSort(output)

def main():

    #biolink = "http://localhost:5000/api" #mart/labels/gene/phenotype/NCBITaxon%3A7955
    biolink = "https://api.monarchinitiative.org/api"

    taxon_map = {
        'Hs': 9606,
        'Mm': 10090,
        'Dr': 7955,
        'Dm': 7227,
        'Ce': 6239
        }

    parser = argparse.ArgumentParser(description='Fetcher from biolink for monarch data',
                                     formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument('-t', '--taxon', type=str, required=False,
                        help='species prefix: ' + ",".join(taxon_map.keys()))
    args = parser.parse_args()

    if args.taxon is None:
        tax_list = taxon_map.keys()
    else:
        tax_list = args.taxon.split(',')

    print("Running for: " + ",".join(tax_list))
    for tax in tax_list:
        if not os.path.exists(tax):
            os.makedirs(tax)

        subjs = ["gene"]
        obj = "phenotype"

        if tax == "Hs":
            subjs = ["disease", "case", "gene"]

        for subj in subjs:
            assocFileJson = tax + "/" + subj + "-" + obj + ".json"
            assocFileTsv = tax + "/" + subj + "-" + obj + ".tsv"
            labelFileTsv = tax + "/" + subj + "-label.tsv"

            assocURL = biolink + "/mart/" + subj + "/" + obj + "/NCBITaxon:" + str(taxon_map.get(tax))
            print("fetching " + assocURL)
            req = requests.get(assocURL)
            data = req.json()
            transformAssociation(data, assocFileTsv)
            transformLabel(data, labelFileTsv)

if __name__ == "__main__":
    main()
