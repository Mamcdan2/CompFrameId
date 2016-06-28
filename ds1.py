# -*- coding: utf-8 -*-
"""
Created on Tue Dec 29 12:43:50 2015

@author: admin
"""
import re
import copy

p = re.compile(r'(\{.+\}|\w+)\s+(\d\.\d+)\s*')
p_split = re.compile(r',\s*')
p_extract = re.compile(r'\{(.+)\}')


def power_set(s):
    return power_set1([[]], s)


def power_set_nonempty(s):
    return power_set1([[]], s)[1:]


def power_set1(s_out, s):
    if len(s) == 0:
        return s_out
    s_out1 = copy.deepcopy(s_out)
    s_out2 = copy.deepcopy(s_out)
    for x in s_out2:
        x.append(s[0])
    s_out1.extend(s_out2)
    return power_set1(s_out1, s[1:])

#Given a dictonary of masses and focal sets (mass)
# outputs a sorted list of all the focal sets in said dictionary as tuples
# without any repeats
def all_from_mass(mass):
    atoms = []
    for k in mass.keys():
        if type(k) is tuple:
            atoms.extend(k)
    return sorted(list(set(atoms)))

#Used to determine whether t1 is a subset of t2

#all is the set of all focal elements and used to check if t2 is actually
#the set of all the focal elements, in which case t1 would always be true
#Needs to be included so that the keyword All can be used to express the set
#of all elements
def t_subset(t1, t2, all):
    # print "   ", t1, "  ", t2, "   ", all
    if t2 == 'All':
        return True
    if t1 == 'All':
        # print t2, "   ", all
        if set(t2) == set(all):
            return True
        else:
            return False
    return set(t1) <= set(t2)

#Used to determine whether the intersection t1 and t2 is the null set
#(returns true if there is some overlap)
def t_overlap(t1, t2):
    if t1 == 'All' or t2 == 'All':
        return True
    return set(t1) & set(t2) != set([])

#Returns the intersection of t1 and t2
#Again, input set "all" is the set of all focal elements to handle the
#keyword "All"
def t_intersection(t1, t2, all):
    if t1 == 'All' and t2 == 'All':
        return set(all)
    if t1 == 'All':
        return set(t2)
    if t2 == 'All':
        return set(t1)
    return set(t1) & set(t2)

#Finds the belief values for the given focal set (set1) given
#   mass: a dictionary of all focal sets and masses
def Bel(set1, mass):
    all = tuple(all_from_mass(mass)) #WHY?
    b = 0.0
    #See if each given set of focal elements is a subset of the input set
    #If so, add its mass to the belief
    for st, val in mass.iteritems():
        if t_subset(st, set1, all):
            b += val
    return b

#Calculates the plausibility of the given focal set (set1) given
#   mass: a dictionary of all focal sets and masses
def Plaus(set1, mass):
    p = 0.0
    #If any set overlaps with the input set, its mass is added to plaus
    for st, val in mass.iteritems():
        if t_overlap(st, set1):
            p += val
    return p

#Formats set of names to be comma separated and bracketed
def to_set_str(in_tuple):
    if in_tuple == 'All':
        return in_tuple
    out_str = "{"
    for i in range(len(in_tuple)-1):
        out_str += str(in_tuple[i]) + ","
    return out_str + str(in_tuple[-1]) + "}"

#Formats name(s) of focal elements into tuples of strings
#All is left as all
#Sets of more than one name are sorted alphabetically
def extract_tuple(in_str):
    if in_str[0] == '{':
        m_extract = p_extract.match(in_str)
        return tuple(sorted(p_split.split(m_extract.group(1))))
    elif in_str == 'All':
        return in_str
    else:
        return (in_str,)

#Reads an input file of masses into a dictionary of focal element:mass pairs
def make_mass(f_name):
    mass = {}
    f = open(f_name, 'r')
    for line in f:
        m = p.match(line)
        if not m:
            print "ill-formed line: ", line
            continue
        name_str, val = m.groups()
        name_tuple = extract_tuple(name_str)
        mass[name_tuple] = float(val)
    return mass


#Prints out a table of mass, belief, plausibility information about a given
#   dictionary of focal sets and masses
def out_measures(mass):
    print "Focal element   Mass  Belief  Plausibility"
    for names, val in mass.iteritems():
        print "{:15s} {:.3f}  {:.3f}  {:.3f}".\
           format(to_set_str(names), val, Bel(names, mass), Plaus(names, mass))

#######NOTE: CURRENTLY ONLY WORKS FOR ONE ELEMENT SETS
#probably could be fixed by not using extract_tuple

#Method to interactively add a set: input a set of name(s) and given an
#existing dictionary of masses computes a belief and plausibility for this
#new set
def interact(mass):
    in_str = raw_input('A set: ')
    in_t = extract_tuple(in_str)
    print in_t
    print "Focal element    Belief  Plausibility"
    print "{:15s}  {:.3f}  {:.3f}".\
        format(to_set_str(in_t), Bel(in_t, mass), Plaus(in_t, mass))

#Combines two dictionaries of masses according to Dempster's Rule
#Calculates a measure of conflict (here denoted by var c)
#   by summing the product of the mass1 and mass2 values for each shared key
#NOTE: this is 1-k, the standard measure of conflict which is for UNshared keys
#Then the new value for a set A is:
#   m(A) = sum(mass1[Ai]*mass2[Ai])/(1-c)
#produces a new dictionary of masses for each key in either mass1 or mass2
def combine(mass1, mass2):
    c = 0
    for k1, v1 in mass1.iteritems():
        for k2, v2 in mass2.iteritems():
            if t_overlap(k1, k2):
                c += v1 * v2
    print c
    atoms = all_from_mass(mass1)
    sts = power_set_nonempty(atoms)
    keys1 = mass1.keys()
    keys2 = mass2.keys()
    mass3 = {}
    for st in sts:
        val = 0.0
        for k1 in keys1:
            for k2 in keys2:
                if t_intersection(k1, k2, atoms) == set(st):
                    val += mass1[k1] * mass2[k2]
        if val != 0.0:
            if set(st) == set(atoms):
                mass3['All'] = val / c
            else:
                mass3[tuple(st)] = val / c
    return mass3

#Combines two dictionaries of masses by using Yager's Conflict to theta Rule
def yager_combine(mass1, mass2):
    k = 0
    for k1, v1 in mass1.iteritems():
        for k2, v2 in mass2.iteritems():
            if not t_overlap(k1, k2):
                k += v1 * v2
    
    atoms = all_from_mass(mass1)
    sts = power_set_nonempty(atoms)
    keys1 = mass1.keys()
    keys2 = mass2.keys()
    mass3 = {}
    for st in sts:
        val = 0.0
        for k1 in keys1:
            for k2 in keys2:
                if t_intersection(k1, k2, atoms) == set(st):
                    val += mass1[k1] * mass2[k2]
        if val != 0.0:
            if set(st) == set(atoms):
                mass3['All'] = val + k
            else:
                mass3[tuple(st)] = val
    return mass3
    
#Combines two dictionaries of masses to get combined *belief* values
    #provided reliablility factors for each dictionary
    #by using the discount and combine rule
#Also prints out a formatted version of the dictionary since the standard
    #out_measures function won't work for a belief dictionary
def discount_and_combine(mass1, mass2, rel1, rel2):
    mass1d = {}  #d is for discounted
    mass2d = {}
    for names in mass1.keys():
        mass1d[names] = Bel(names, mass1)*rel1
    for names in mass2.keys():
        mass2d[names] = Bel(names, mass2)*rel2
        
    #Now I just need to average the data     
    bel3 = {}
    for names in mass1d.keys():
        if names in mass2d:
            bel3[names] = (mass1d[names] + mass2d[names])/2.0
        else:
            bel3[names] = (mass1d[names])/2.0
    for names, val in mass2d.iteritems():
        if names not in bel3:
            bel3[names] = val/2.0

    #The belieif in all values should still be 1
    bel3['All'] = 1    
    
    for names, val in mass3.iteritems():
        print "{:15s} {:.3f}".\
           format(to_set_str(names), val)
    return bel3

#Combines two dictionaries of masses with provided reliablility factors
#for each dictionary by using the mixing/averaging rule
#Similar to discount+combine but with masses, not beliefs
def mixing_combine(mass1, mass2, rel1, rel2):
    mass3 = {}
    for names in mass1.keys():
        if names in mass2:
            mass3[names] = (rel1*mass1[names] + rel2*mass2[names])/2.0
        else:
            mass3[names] = (rel1*mass1[names])/2.0
    for names, val in mass2.iteritems():
        if names not in mass3:
            mass3[names] = rel2*val/2.0
    return mass3
    
#NOTE: I hope that this is an accurate assement of this rule, but I'd feel better
    #checking it against an example if I can find one
#Combines two dictionaries of masses according to Zhang's center combination rule
def zhang_combine(mass1, mass2):
    keys1 = mass1.keys()
    keys2 = mass2.keys()
    
    #Doing a lazy intersection of the two sets just in case there are different
    #suspects between the two mass dicts: empty set because we have no "All"
    atoms = list(t_intersection(all_from_mass(mass1), all_from_mass(mass2), []))
    #atoms = all_from_mass(mass1)
    sts = power_set_nonempty(atoms)
    keys1 = mass1.keys()
    keys2 = mass2.keys()
    mass3 = {}
    
    #the only sets where the intersection is all is all from both m1 and m2
    mass3["All"] = mass1["All"] * mass2["All"] / len(atoms)
    
    for st in sts:
        val = 0.0
        for k1 in keys1:
            for k2 in keys2:
                if t_intersection(k1, k2, atoms) == set(st):
                    val += mass1[k1] * mass2[k2] * \
                    len(t_intersection(k1, k2, atoms)) / len(k1) / len(k2)
        if val != 0.0:
            if set(st) != set(atoms):
                mass3[tuple(st)] = val

    #Now need to go through mass3 and renormalize
    k = sum(mass3.values())
    for n in mass3.keys():
        mass3[n]/=k
    return mass3

print "Testing that this import worked"
##mass1 = make_mass('fp_mass.txt')
###This should always be 1: sum of all the masses input
##print "The sum is %.3f" % sum(mass1.values())
##print "Information from fingerprint data"
##out_measures(mass1)
##while True:
##    in_str = raw_input("Values for another set (y/n)? ")
##    if in_str.lower()[0] == 'n':
##        break
##    interact(mass1)
##
##mass2 = make_mass('mug_mass.txt')
##print "The sum is %.3f" % sum(mass2.values())
##print "Information from mug shots"
##out_measures(mass2)
##while True:
##    in_str = raw_input("Values for another set (y/n)? ")
##    if in_str.lower()[0] == 'n':
##        break
##    interact(mass2)
##
##mass3 = combine(mass1, mass2)
##print "The sum is %.3f" % sum(mass3.values())
##print "Combined mass data (using Dempster's Rule)"
##out_measures(mass3)
##while True:
##    in_str = raw_input("Values for another set (y/n)? ")
##    if in_str.lower()[0] == 'n':
##        break
##    interact(mass3)
##    
##mass4 = yager_combine(mass1, mass2)
##print "The sum is %.3f" % sum(mass4.values())
##print "Combined mass data (using Yager's Rule)"
##out_measures(mass4)
##
